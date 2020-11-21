package insidebot;

import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectSet;
import arc.util.*;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.guild.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import discord4j.rest.util.Color;
import insidebot.EventType.*;
import insidebot.data.dao.*;
import insidebot.data.model.*;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import static insidebot.AuditEventType.*;
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

    // Регистрируем ивентики
    protected void register(){
        listener.guild = listener.gateway.getGuildById(guildID).block();

        gateway.on(ReadyEvent.class).subscribe(event -> {
            Log.info("Bot up.");
        });

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            User user = event.getMessage().getAuthor().orElse(null);
            if(user == null) return;
            Message message = event.getMessage();
            MessageInfo info = new MessageInfo();
            UserInfo userInfo = UserInfoDao.getOr(user.getId(), UserInfo::new);

            userInfo.setName(user.getUsername());
            userInfo.setUserId(user.getId().asLong());
            userInfo.setLastSentMessage(Calendar.getInstance());
            userInfo.addToSeq();

            info.setUser(userInfo);
            info.setMessageId(message.getId().asLong());
            info.setChannelId(message.getChannelId().asLong());
            info.setTimestamp(Calendar.getInstance());

            info.setContent(effectiveContent(message));
            userInfo.getMessageInfo().add(info);

            commands.handle(event);
            UserInfoDao.saveOrUpdate(userInfo);
        }, Log::err);

        gateway.on(MessageUpdateEvent.class).subscribe(event -> {
            Message message = event.getMessage().block();

            User user = message.getAuthor().orElse(null);
            if(user == null || user.isBot()) return;
            if(!UserInfoDao.exists(event.getMessageId())) return;

            EmbedCreateSpec embed = new EmbedCreateSpec();
            MessageInfo info = MessageInfoDao.get(event.getMessageId());

            String oldContent = info.getContent();
            String newContent = effectiveContent(message);
            boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

            if(message.isPinned() || newContent.equals(oldContent)) return;

            embed.setColor(messageEdit.color);
            embed.setAuthor(memberedName(user), null, user.getAvatarUrl());
            embed.setTitle(bundle.format("message.edit", event.getChannel().block().getMention()));
            embed.setDescription(bundle.format(event.getGuildId().isPresent() ? "message.edit.description" : "message.edit.nullable-guild",
                                               event.getGuildId().get().asLong(), /* Я не знаю как такое получить, но всё же обезопашусь */
                                               event.getChannelId().asLong(),
                                               event.getMessageId().asLong()));

            embed.addField(bundle.get("message.edit.old-content"),
                           MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
            embed.addField(bundle.get("message.edit.new-content"),
                           MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

            embed.setFooter(data.zonedFormat(), null);

            if(under){
                temp.writeString(String.format("%s:\n%s\n\n%s:\n%s",
                                               bundle.get("message.edit.old-content"), oldContent,
                                               bundle.get("message.edit.new-content"), newContent));
            }

            log(embed, under);

            info.setContent(newContent);
            MessageInfoDao.update(info);
        }, Log::err);

        gateway.on(MessageDeleteEvent.class).subscribe(event -> {
            if(!MessageInfoDao.exists(event.getMessageId())) return;
            if(buffer.contains(event.getMessageId())){
                buffer.remove(event.getMessageId());
                return;
            }

            EmbedCreateSpec embed = new EmbedCreateSpec();
            MessageInfo info = MessageInfoDao.get(event.getMessageId());

            User user = info.getUser().asUser();
            String content = info.getContent();
            boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

            if(content.isEmpty()) return;

            embed.setColor(messageDelete.color);
            embed.setAuthor(memberedName(user), null, user.getAvatarUrl());
            embed.setTitle(bundle.format("message.delete", event.getChannel().block().getMention()));
            embed.setFooter(data.zonedFormat(), null);

            embed.addField(bundle.get("message.delete.content"), MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);

            if(under){
                temp.writeString(String.format("%s:\n%s", bundle.get("message.delete.content"), content));
            }

            log(embed, under);

            MessageInfoDao.remove(info);
        }, Log::err);

        gateway.on(VoiceStateUpdateEvent.class).subscribe(event -> {
            VoiceChannel channel = event.getCurrent().getChannel().block();
            User user = event.getCurrent().getUser().block();
            if(user == null || user.isBot() || channel == null) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceJoin.color);
                embedBuilder.setTitle(bundle.get("message.voice-join"));
                embedBuilder.setDescription(bundle.format("message.voice-join.text", memberedName(user), channel.getName()));
                embedBuilder.setFooter(data.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(VoiceStateUpdateEvent.class).subscribe(event -> { /* Может совместить с ивентом выше? */
            if(event.getOld().isPresent()) return;
            VoiceChannel channel = event.getOld().get().getChannel().block();
            User user = event.getOld().get().getUser().block();
            if(user == null || user.isBot() || channel == null) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceLeave.color);
                embedBuilder.setTitle(bundle.get("message.voice-leave"));
                embedBuilder.setDescription(bundle.format("message.voice-leave.text", memberedName(user), channel.getName()));
                embedBuilder.setFooter(data.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(MemberJoinEvent.class).subscribe(event -> {
            User user = gateway.getUserById(event.getMember().getId()).block();
            if(user == null || user.isBot()) return;
            log(embedBuilder -> {
                embedBuilder.setColor(userJoin.color);
                embedBuilder.setTitle(bundle.get("message.user-join"));
                embedBuilder.setDescription(bundle.format("message.user-join.text", user.getUsername()));
                embedBuilder.setFooter(data.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(MemberLeaveEvent.class).subscribe(event -> {
            User user = event.getUser();
            if(user.isBot()) return;

            UserInfoDao.removeById(user.getId());
            log(embedBuilder -> {
                embedBuilder.setColor(userLeave.color);
                embedBuilder.setTitle(bundle.get("message.user-leave"));
                embedBuilder.setDescription(bundle.format("message.user-leave.text", user.getUsername()));
                embedBuilder.setFooter(data.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(BanEvent.class).subscribe(event -> {
            User user = event.getUser();
            if(user.isBot()) return;

            UserInfoDao.removeById(user.getId());
            log(embedBuilder -> {
                embedBuilder.setColor(userBan.color);
                embedBuilder.setTitle(bundle.get("message.ban"));
                embedBuilder.setDescription(bundle.format("message.ban.text", user.getUsername()));
                embedBuilder.setFooter(data.zonedFormat(), null);
            });
        }, Log::err);

        gateway.on(MemberUpdateEvent.class).subscribe(event -> {
            User user = gateway.getUserById(event.getMemberId()).block();
            if(user == null || user.isBot()) return;
            if(!UserInfoDao.exists(user.getId())) return;

            UserInfo info = UserInfoDao.get(user.getId());
            event.getCurrentNickname().ifPresent(info::setName); // может в холостую сработать, ну а что поделать
            UserInfoDao.update(info);
        }, Log::err);

        // Внутренние ивенты

        Events.on(MemberUnmuteEvent.class, event -> {
            Member member = event.userInfo.asMember();
            if(member == null) return;

            event.userInfo.setMuteEndDate(null);
            UserInfoDao.update(event.userInfo);
            member.removeRole(muteRoleID).block();
            log(e -> {
                e.setTitle(bundle.get("message.unmute"));
                e.setDescription(bundle.format("message.unmute.text", event.userInfo.getName()));
                e.setFooter(data.zonedFormat(), null);
                e.setColor(userUnmute.color);
            });
        });

        Events.on(MemberMuteEvent.class, event -> {
            Member member = guild.getMemberById(event.user.getId()).block();
            UserInfo userInfo = UserInfoDao.get(event.user.getId());
            if(member == null || userInfo == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
            userInfo.setMuteEndDate(calendar);
            UserInfoDao.update(userInfo);
            member.addRole(muteRoleID).block();
            log(embedBuilder -> {
                embedBuilder.setTitle(bundle.get("message.mute"));
                embedBuilder.setDescription(bundle.format("message.mute.text", event.user.getMention(), event.delay));
                embedBuilder.setFooter(data.zonedFormat(), null);
                embedBuilder.setColor(userMute.color);
            });
        });

        Events.on(MessageClearEvent.class, event -> {
            Flux.fromIterable(event.history).subscribe(m -> {
                buffer.add(m.getId());
                m.delete();
            });

            log(embedBuilder -> {
                String channel = event.channel.getMention();
                embedBuilder.setTitle(bundle.format("message.clear", event.count, channel));
                embedBuilder.setDescription(bundle.format("message.clear.text", event.user.getUsername(), event.count, channel));
                embedBuilder.setFooter(data.zonedFormat(), null);
                embedBuilder.setColor(messageClear.color);

                StringBuilder builder = new StringBuilder();
                event.history.forEach(m -> {
                    buffer.add(m.getId());
                    builder.append('[').append(dateTime.withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
                    builder.append(m.getUserData().username()).append(" = ");
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

        Events.on(MemberBanEvent.class, event -> { // переадресирует на ивент от d4j. oh no кандидат на чистку
            guild.ban(Snowflake.of(event.userInfo.getUserId()), b -> b.setDeleteMessageDays(0)).block();
            UserInfoDao.remove(event.userInfo);
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

    public void log(EmbedCreateSpec embed){
        log(e -> e = embed, false);
    }

    public void log(Consumer<EmbedCreateSpec> embed){
        log(embed, false);
    }

    public void log(EmbedCreateSpec embed, boolean file){
        log(e -> e = embed, file);
    }

    public void log(Consumer<EmbedCreateSpec> embed, boolean file){
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        log(file ? m.addFile("message", temp.read()) : m);
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
            name += " / " + member.getNickname();
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
