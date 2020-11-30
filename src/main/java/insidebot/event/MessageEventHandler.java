package insidebot.event;

import arc.struct.ObjectSet;
import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.*;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import discord4j.discordjson.json.MessageData;
import insidebot.Settings;
import insidebot.audit.AuditEventHandler;
import insidebot.common.command.model.base.CommandReference;
import insidebot.common.command.service.*;
import insidebot.common.command.service.BaseCommandHandler.*;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;
import reactor.core.publisher.Mono;

import java.util.Calendar;
import java.util.function.Consumer;

import static insidebot.audit.AuditEventType.*;

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
        Member member = event.getMember().orElse(null);
        if(member == null) return Mono.empty();
        TextChannel channel = message.getChannel().cast(TextChannel.class).block();
        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null || channel == null) return Mono.empty();
        Snowflake userId = user.getId();
        LocalMember localMember = memberService.getOr(member, LocalMember::new);

        if(!guildService.exists(guildId)){
            Region region = event.getGuild().flatMap(Guild::getRegion).block();
            GuildConfig guildConfig = new GuildConfig(guildId, LocaleUtil.get(region), settings.prefix);
            guildService.save(guildConfig);
        }

        context.init(guildId);

        if(localMember.user() == null){
            LocalUser localUser = userService.getOr(userId, LocalUser::new);
            localUser.userId(userId);
            localUser.name(user.getUsername());
            localUser.discriminator(user.getDiscriminator());
            localMember.user(localUser);
            userService.save(localUser);
        }

        localMember.effectiveName(member.getDisplayName());
        localMember.id(userId);
        localMember.guildId(guildId);
        localMember.lastSentMessage(Calendar.getInstance());
        localMember.addToSeq();

        if(!MessageUtil.isEmpty(message) && !message.isTts() && message.getEmbeds().isEmpty()){
            MessageInfo info = new MessageInfo();
            info.userId(userId);
            info.id(message.getId());
            info.guildId(guildId);
            info.channelId(message.getChannelId());
            info.timestamp(Calendar.getInstance());
            info.content(MessageUtil.effectiveContent(message));
            messageService.save(info);
        }

        CommandReference reference = new CommandReference()
                .localMember(localMember)
                .localUser(localMember.user())
                .member(member)
                .user(user);

        if(memberService.isAdmin(member)){
            handleResponse(commandHandler.handleMessage(message.getContent(), reference, event), channel);
        }
        memberService.save(localMember);
        return Mono.empty();
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Message message = event.getMessage().block();
        if(message == null) return Mono.empty();
        User user = message.getAuthor().orElse(null);
        TextChannel c = message.getChannel().cast(TextChannel.class).block();
        if(DiscordUtil.isBot(user) || c == null) return Mono.empty();
        if(!messageService.exists(event.getMessageId())) return Mono.empty();

        MessageInfo info = messageService.getById(event.getMessageId());

        String oldContent = info.content();
        String newContent = MessageUtil.effectiveContent(message);
        boolean under = newContent.length() >= Embed.Field.MAX_VALUE_LENGTH || oldContent.length() >= Embed.Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)) return Mono.empty();

        context.init(info.guildId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageEdit.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.edit", c.getName()));
            embed.setDescription(messageService.format(event.getGuildId().isPresent() ? "message.edit.description" : "message.edit.nullable-guild",
                                                       event.getGuildId().get().asString(), /* Я не знаю как такое получить, но всё же обезопашусь */
                                                       event.getChannelId().asString(),
                                                       event.getMessageId().asString()));

            embed.addField(messageService.get("message.edit.old-content"),
                           MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), false);
            embed.addField(messageService.get("message.edit.new-content"),
                           MessageUtil.substringTo(newContent, Embed.Field.MAX_VALUE_LENGTH), true);

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
        if(m == null) return Mono.empty();
        Guild guild = m.getGuild().block();
        TextChannel c =  event.getChannel().cast(TextChannel.class).block();
        if(guild == null || c == null) return Mono.empty();
        if(c.getId().equals(guildService.logChannelId(guild.getId())) && !m.getEmbeds().isEmpty()){ /* =) */
            AuditLogEntry l = guild.getAuditLog().filter(a -> a.getActionType() == ActionType.MESSAGE_DELETE).blockFirst();
            return Mono.justOrEmpty(l).doOnNext(a -> {
                log.warn("User '{}' deleted log message", guild.getMemberById(a.getResponsibleUserId()).block().getUsername());
            }).then();
        }
        if(!messageService.exists(event.getMessageId())) return Mono.empty();
        if(buffer.contains(event.getMessageId())){
            return Mono.fromRunnable(() -> buffer.remove(event.getMessageId()));
        }

        MessageInfo info = messageService.getById(m.getId());
        User user = m.getAuthor().orElse(null);
        String content = info.content();
        boolean under = content.length() >= Embed.Field.MAX_VALUE_LENGTH;

        if(DiscordUtil.isBot(user) || MessageUtil.isEmpty(content)) return Mono.empty();

        context.init(guild.getId());

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageDelete.color);
            embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.delete", c.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.addField(messageService.get("message.delete.content"), MessageUtil.substringTo(content, Embed.Field.MAX_VALUE_LENGTH), true);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:\n%s", messageService.get("message.delete.content"), content));
        }

        log(guild.getId(), e, under);

        messageService.delete(info);
        return Mono.empty();
    }

    @Override
    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        MessageData data = discordService.getLogChannel(guildId)
                                         .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                                         .block();
        return Mono.justOrEmpty(data).flatMap(__ -> Mono.fromRunnable(() -> context.reset()));
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
