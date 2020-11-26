package insidebot;

import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectSet;
import arc.util.*;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.domain.*;
import discord4j.core.event.domain.guild.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.VoiceState;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import discord4j.rest.util.Color;
import insidebot.EventType.*;
import insidebot.data.entity.*;
import insidebot.data.services.*;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import static insidebot.audit.AuditEventType.*;
import static insidebot.InsideBot.*;

public class Listener{
    public Guild guild;
    public DiscordClient client;
    public GatewayDiscordClient gateway;
    public Color normalColor = Color.of(0xC4F5B7), errorColor = Color.of(0xff3838);
    public Fi temp = new Fi("message.txt");

    // привет костыль (на самом деле не совсем то и костыль)
    public ObjectSet<Snowflake> buffer = new ObjectSet<>();

    TextChannel channel;
    User lastUser;
    Message lastMessage, lastSentMessage;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    protected void register(){
        guild = gateway.getGuildById(guildID).block();

        gateway.on(ReadyEvent.class).subscribe(event -> {
            Log.info("Bot up.");
        });

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            User user = event.getMessage().getAuthor().orElse(null);
            if(user == null || user.isBot()) return;
            Message message = event.getMessage();
            MessageInfo info = new MessageInfo();
            UserInfo userInfo = userService.getOr(user.getId(), UserInfo::new);

            userInfo.name(user.getUsername());
            userInfo.userId(user.getId());
            userInfo.lastSentMessage(Calendar.getInstance());
            userInfo.addToSeq();

            info.user(userInfo);
            info.id(message.getId());
            info.guildId(message.getGuildId().orElseThrow(RuntimeException::new)); // Не надо мне тут
            info.channelId(message.getChannelId());
            info.timestamp(Calendar.getInstance());

            info.content(effectiveContent(message));
            userInfo.messageInfo().add(info);

            commands.handle(event);
            userService.save(userInfo);
        }, Log::err);

        gateway.on(MessageUpdateEvent.class).subscribe(event -> {
            Message message = event.getMessage().block();
            TextChannel c = message.getChannel().cast(TextChannel.class).block();
            User user = message.getAuthor().orElse(null);
            if(DiscordUtil.isBot(user)) return;
            if(!messageService.exists(event.getMessageId())) return;

            MessageInfo info = messageService.getById(event.getMessageId());

            String oldContent = info.content();
            String newContent = effectiveContent(message);
            boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

            if(message.isPinned() || newContent.equals(oldContent)) return;

            Consumer<EmbedCreateSpec> e = embed -> {
                embed.setColor(messageEdit.color);
                embed.setAuthor(memberedName(user), null, user.getAvatarUrl());
                embed.setTitle(bundle.format("message.edit", c.getName()));
                embed.setDescription(bundle.format(event.getGuildId().isPresent() ? "message.edit.description" : "message.edit.nullable-guild",
                                                   event.getGuildId().get().asString(), /* Я не знаю как такое получить, но всё же обезопашусь */
                                                   event.getChannelId().asString(),
                                                   event.getMessageId().asString()));

                embed.addField(bundle.get("message.edit.old-content"),
                               MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
                embed.addField(bundle.get("message.edit.new-content"),
                               MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

                embed.setFooter(MessageUtil.zonedFormat(), null);
            };

            if(under){
                temp.writeString(String.format("%s:\n%s\n\n%s:\n%s",
                                               bundle.get("message.edit.old-content"), oldContent,
                                               bundle.get("message.edit.new-content"), newContent));
            }

            log(e, under);

            info.content(newContent);
            messageService.save(info);
        }, Log::err);

        gateway.on(MessageDeleteEvent.class).subscribe(event -> {
            Message m = event.getMessage().orElse(null);
            if(event.getChannelId().equals(logChannelID) && (m != null && !m.getEmbeds().isEmpty())){ /* =) */
                AuditLogEntry l = guild.getAuditLog().filter(a -> a.getActionType() == ActionType.MESSAGE_DELETE).blockFirst();
                if(l != null){
                    Log.warn("User '@' deleted log message", gateway.getUserById(l.getResponsibleUserId()).block().getUsername());
                }
                return;
            }
            if(!messageService.exists(event.getMessageId())) return;
            if(buffer.contains(event.getMessageId())){
                buffer.remove(event.getMessageId());
                return;
            }

            MessageInfo info = messageService.getById(event.getMessageId());
            User user = info.user().asUser().block();
            TextChannel c = event.getChannel().cast(TextChannel.class).block();
            String content = info.content();
            boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

            if(c == null || MessageUtil.isEmpty(content)) return;

            Consumer<EmbedCreateSpec> e = embed -> {
                embed.setColor(messageDelete.color);
                embed.setAuthor(memberedName(user), null, user.getAvatarUrl());
                embed.setTitle(bundle.format("message.delete", c.getName()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
                embed.addField(bundle.get("message.delete.content"), MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
            };

            if(under){
                temp.writeString(String.format("%s:\n%s", bundle.get("message.delete.content"), content));
            }

            log(e, under);

            messageService.delete(info);
        }, Log::err);

        gateway.on(VoiceStateUpdateEvent.class).subscribe(event -> {
            VoiceChannel channel = event.getCurrent().getChannel().block();
            User user = event.getCurrent().getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceJoin.color);
                embedBuilder.setTitle(bundle.get("message.voice-join"));
                embedBuilder.setDescription(bundle.format("message.voice-join.text", memberedName(user), channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(VoiceStateUpdateEvent.class).subscribe(event -> { /* Может совместить с ивентом выше? */
            VoiceState state = event.getOld().orElse(null);
            VoiceChannel channel = state != null ? state.getChannel().block() : null;
            User user = state != null ? state.getUser().block() : null;
            if(DiscordUtil.isBot(user) || channel == null) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceLeave.color);
                embedBuilder.setTitle(bundle.get("message.voice-leave"));
                embedBuilder.setDescription(bundle.format("message.voice-leave.text", memberedName(user), channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(MemberJoinEvent.class).subscribe(event -> {
            User user = gateway.getUserById(event.getMember().getId()).block();
            if(DiscordUtil.isBot(user)) return;
            log(embedBuilder -> {
                embedBuilder.setColor(userJoin.color);
                embedBuilder.setTitle(bundle.get("message.user-join"));
                embedBuilder.setDescription(bundle.format("message.user-join.text", user.getUsername()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(MemberLeaveEvent.class).subscribe(event -> {
            User user = event.getUser();
            if(DiscordUtil.isBot(user)) return;

            log(embedBuilder -> {
                embedBuilder.setColor(userLeave.color);
                embedBuilder.setTitle(bundle.get("message.user-leave"));
                embedBuilder.setDescription(bundle.format("message.user-leave.text", user.getUsername()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
            userService.deleteById(user.getId());
        }, Log::err);

        gateway.on(BanEvent.class).subscribe(event -> {
            User user = event.getUser();
            if(DiscordUtil.isBot(user)) return;

            log(embedBuilder -> {
                embedBuilder.setColor(userBan.color);
                embedBuilder.setTitle(bundle.get("message.ban"));
                embedBuilder.setDescription(bundle.format("message.ban.text", user.getUsername()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
            userService.deleteById(user.getId());
        }, Log::err);

        gateway.on(UserUpdateEvent.class).subscribe(event -> {
            User user = event.getCurrent();
            if(DiscordUtil.isBot(user)) return;
            if(!messageService.exists(user.getId())) return;

            UserInfo info = userService.getById(user.getId());
            info.name(user.getUsername());
            userService.save(info);
        }, Log::err);

        // Внутренние ивенты

        Events.on(MemberUnmuteEvent.class, event -> {
            Member member = event.userInfo.asMember().block();
            if(DiscordUtil.isBot(member)) return;

            event.userInfo.muteEndDate(null);
            userService.delete(event.userInfo);
            member.removeRole(muteRoleID).block();
            log(e -> {
                e.setTitle(bundle.get("message.unmute"));
                e.setDescription(bundle.format("message.unmute.text", event.userInfo.name()));
                e.setFooter(MessageUtil.zonedFormat(), null);
                e.setColor(userUnmute.color);
            });
        });

        Events.on(MemberMuteEvent.class, event -> {
            Member member = guild.getMemberById(event.user.getId()).block();
            UserInfo userInfo = userService.getById(event.user.getId());
            if(DiscordUtil.isBot(member) || userInfo == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
            userInfo.muteEndDate(calendar);
            userService.save(userInfo);
            member.addRole(muteRoleID).block();
            log(embedBuilder -> {
                embedBuilder.setTitle(bundle.get("message.mute"));
                embedBuilder.setDescription(bundle.format("message.mute.text", event.user.getMention(), event.delay));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
                embedBuilder.setColor(userMute.color);
            });
        });

        Events.on(MessageClearEvent.class, event -> {
            Flux.fromIterable(event.history).subscribe(m -> {
                buffer.add(m.getId());
                m.delete().block();
            });

            log(embed -> {
                embed.setTitle(bundle.format("message.clear", event.count, event.channel.getName()));
                embed.setDescription(bundle.format("message.clear.text", event.user.getUsername(), event.count, event.channel.getName()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
                embed.setColor(messageClear.color);

                StringBuilder builder = new StringBuilder();
                event.history.forEach(m -> {
                    builder.append('[').append(dateTime.withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
                    builder.append(m.getUserData().username()).append(" > ");
                    builder.append(m.getContent());
                    if(!m.getAttachments().isEmpty()){
                        builder.append("\n---\n");
                        m.getAttachments().forEach(a -> builder.append(a.getUrl()).append('\n'));
                    }
                    builder.append('\n');
                });
                temp.writeString(builder.toString());
            }, true);
        });
    }

    // utils

    public void text(String text, Object... args){
        lastSentMessage = channel.createMessage(Strings.format(text, args)).block();
    }

    public void info(String title, String text, Object... args){
        lastSentMessage = channel.createMessage(s -> s.setEmbed(e -> e.setColor(normalColor).setTitle(title)
                                                                      .setDescription(Strings.format(text, args)))).block();
    }

    public void err(String text, Object... args){
        err(bundle.get("error"), text, args);
    }

    public void err(String title, String text, Object... args){
        lastSentMessage = channel.createMessage(s -> s.setEmbed(e -> e.setColor(errorColor).setTitle(title)
                                                                      .setDescription(Strings.format(text, args)))).block();
    }

    public void log(Consumer<EmbedCreateSpec> embed){
        log(embed, false);
    }

    public void log(Consumer<EmbedCreateSpec> embed, boolean file){
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        log(file ? m.addFile("message.txt", temp.read()) : m);
    }

    public void log(@NonNull MessageCreateSpec message){
        guild.getChannelById(logChannelID).cast(TextChannel.class).block()
             .getRestChannel().createMessage(message.asRequest()).block();
    }

    // username / membername
    public String memberedName(@NonNull User user){
        String name = user.getUsername();
        Member member = guild.getMemberById(user.getId()).block();
        if(member != null && member.getNickname().isPresent()){
            name += " / " + member.getNickname().get();
        }
        return name;
    }

    public String effectiveContent(@NonNull Message message){
        StringBuilder builder = new StringBuilder(message.getContent());
        if(!message.getAttachments().isEmpty()){
            builder.append("\n---\n");
            message.getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
        }
        return builder.toString();
    }
}
