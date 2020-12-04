package insidebot.event;

import arc.struct.ObjectSet;
import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.Region;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import insidebot.Settings;
import insidebot.event.audit.*;
import insidebot.common.command.model.base.CommandReference;
import insidebot.common.command.service.*;
import insidebot.common.command.service.BaseCommandHandler.*;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Calendar;
import java.util.function.Consumer;

import static insidebot.event.audit.AuditEventType.*;

@Component
public class MessageEventHandler extends AuditEventHandler{
    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private GuildService guildService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private Settings settings;

    @Autowired
    private Logger log;

    public final ObjectSet<Snowflake> buffer = new ObjectSet<>();

    @Override
    public Publisher<?> onReady(ReadyEvent event){ // не триггерится, баг текущей версии d4j
        return Mono.fromRunnable(() -> log.info("Bot up."));
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        String text = message.getContent();
        if(text.toLowerCase().contains("egg")){ /* egg */
            message.addReaction(ReactionEmoji.unicode("\uD83E\uDD5A")).block();
        }
        Member member = event.getMember().orElse(null);
        if(member == null || message.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT) return Mono.empty();
        TextChannel channel = message.getChannel().cast(TextChannel.class).block();
        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null || channel == null) return Mono.empty();
        Snowflake userId = user.getId();
        LocalMember localMember = memberService.getOr(member, () -> new LocalMember(member));

        if(!guildService.exists(guildId)){
            Region region = event.getGuild().flatMap(Guild::getRegion).block();
            GuildConfig guildConfig = new GuildConfig(guildId, LocaleUtil.get(region), settings.prefix);
            guildService.save(guildConfig);
        }

        context.init(guildId);

        if(localMember.user() == null){
            LocalUser localUser = userService.getOr(userId, () -> new LocalUser(user));
            localMember.user(localUser);
            userService.save(localUser);
        }

        localMember.lastSentMessage(Calendar.getInstance());
        localMember.addToSeq();
        memberService.save(localMember);

        if(!MessageUtil.isEmpty(message) && !message.isTts() && message.getEmbeds().isEmpty()){
            MessageInfo info = new MessageInfo();
            info.userId(userId);
            info.messageId(message.getId());
            info.guildId(guildId);
            info.timestamp(Calendar.getInstance());
            info.content(MessageUtil.effectiveContent(message));
            messageService.save(info);
        }

        CommandReference reference = new CommandReference()
                .localMember(localMember)
                .localUser(localMember.user())
                .member(member)
                .user(user);

        if(adminService.isAdmin(member)){
            handleResponse(commandHandler.handleMessage(text, reference, event), channel);
        }
        return Mono.empty();
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Message message = event.getMessage().block();
        if(message == null || message.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT) return Mono.empty();
        User user = message.getAuthor().orElse(null);
        TextChannel c = message.getChannel().cast(TextChannel.class).block();
        if(DiscordUtil.isBot(user) || c == null) return Mono.empty();
        if(!messageService.exists(message.getId())) return Mono.empty();

        MessageInfo info = messageService.getById(event.getMessageId());

        String oldContent = info.content();
        String newContent = MessageUtil.effectiveContent(message);
        boolean under = newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)) return Mono.empty();

        context.init(info.guildId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageEdit.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.edit", c.getName()));
            embed.setDescription(messageService.format(event.getGuildId().isPresent() ? "message.edit.description" : "message.edit.nullable-guild",
                                                       event.getGuildId().map(Snowflake::asString).orElse(null), /* Я не знаю как такое получить, но всё же обезопашусь */
                                                       event.getChannelId().asString(),
                                                       event.getMessageId().asString()));

            embed.addField(messageService.get("message.edit.old-content"),
                           MessageUtil.substringTo(oldContent, Field.MAX_VALUE_LENGTH), false);
            embed.addField(messageService.get("message.edit.new-content"),
                           MessageUtil.substringTo(newContent, Field.MAX_VALUE_LENGTH), true);

            embed.setFooter(MessageUtil.zonedFormat(), null);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:\n%s\n\n%s:\n%s",
                                                        messageService.get("message.edit.old-content"), oldContent,
                                                        messageService.get("message.edit.new-content"), newContent));
        }

        log(c.getGuildId(), e, under);

        info.content(newContent);
        messageService.save(info);
        return Mono.empty();
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        Message m = event.getMessage().orElse(null);
        if(m == null || m.getChannel().map(Channel::getType).block() != Type.GUILD_TEXT) return Mono.empty();
        Guild guild = m.getGuild().block();
        TextChannel c =  event.getChannel().cast(TextChannel.class).block();
        if(guild == null || c == null) return Mono.empty();
        if(c.getId().equals(guildService.logChannelId(guild.getId())) && !m.getEmbeds().isEmpty()){ /* =) */
            AuditLogEntry l = guild.getAuditLog().filter(a -> a.getActionType() == ActionType.MESSAGE_DELETE).blockFirst();
            return Mono.justOrEmpty(l).doOnNext(a -> {
                log.warn("User '{}' deleted log message", guild.getMemberById(a.getResponsibleUserId()).block().getUsername());
            }).then();
        }
        if(!messageService.exists(m.getId())) return Mono.empty();
        if(buffer.contains(m.getId())){
            return Mono.fromRunnable(() -> buffer.remove(event.getMessageId()));
        }

        MessageInfo info = messageService.getById(m.getId());
        User user = m.getAuthor().orElse(null);
        String content = info.content();
        boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

        if(DiscordUtil.isBot(user) || MessageUtil.isEmpty(content)) return Mono.empty();

        context.init(guild.getId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageDelete.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.delete", c.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.addField(messageService.get("message.delete.content"), MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:\n%s", messageService.get("message.delete.content"), content));
        }

        log(guild.getId(), e, under);

        messageService.delete(info);
        return Mono.empty();
    }

    protected void handleResponse(CommandResponse response, TextChannel channel){
        String prefix = guildService.prefix(channel.getGuildId());
        if(response.type == BaseCommandHandler.ResponseType.unknownCommand){
            int min = 0;
            Command closest = null;

            for(Command command : commandHandler.commandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < min)){
                    min = dst;
                    closest = command;
                }
            }

            if(closest != null){
                messageService.err(channel, messageService.format("command.response.found-closest", closest.text));
            }else{
                messageService.err(channel, messageService.format("command.response.unknown", prefix));
            }
        }else if(response.type == BaseCommandHandler.ResponseType.manyArguments){
            messageService.err(channel, messageService.get("command.response.many-arguments"),
                               messageService.format("command.response.many-arguments.text",
                                                     prefix, response.command.text, response.command.paramText));
        }else if(response.type == BaseCommandHandler.ResponseType.fewArguments){
            messageService.err(channel, messageService.get("command.response.few-arguments"),
                               messageService.format("command.response.few-arguments.text",
                                                     prefix, response.command.text));
        }
    }
}
