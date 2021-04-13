package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.AuditEntryInfoData;
import discord4j.discordjson.possible.Possible;
import inside.command.CommandHandler;
import inside.command.model.CommandEnvironment;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import inside.event.audit.*;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.function.Tuples;

import java.time.Instant;

import static inside.event.MemberEventHandler.TIMEOUT_MILLIS;
import static inside.event.audit.Attribute.*;
import static inside.event.audit.AuditActionType.*;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

@Component
public class MessageEventHandler extends ReactiveEventAdapter{
    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AuditService auditService;

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        if(DiscordUtil.isBot(member) || MessageUtil.isEmpty(message) || message.getType() != Message.Type.DEFAULT || message.isTts() || !message.getEmbeds().isEmpty()){
            return Mono.empty();
        }

        Snowflake guildId = event.getGuildId().orElseThrow(IllegalStateException::new); // Guaranteed above, see DiscordUtil#isBot

        LocalMember localMember = entityRetriever.getMember(member);

        DateTime time = new DateTime(message.getTimestamp().toEpochMilli());
        localMember.lastSentMessage(time);
        entityRetriever.save(localMember);

        Mono<Void> safeMessageInfo = Mono.fromRunnable(() -> {
            MessageInfo info = new MessageInfo();
            info.userId(member.getId());
            info.messageId(message.getId());
            info.guildId(guildId);
            info.timestamp(time);
            info.content(messageService.encrypt(MessageUtil.effectiveContent(message), message.getId(), message.getChannelId()));
            messageService.save(info);
        });

        Context context = Context.of(KEY_LOCALE, entityRetriever.getLocale(guildId),
                KEY_TIMEZONE, entityRetriever.getTimeZone(guildId));

        CommandEnvironment environment = CommandEnvironment.builder()
                .message(message)
                .member(member)
                .context(context)
                .localMember(localMember)
                .build();

        return commandHandler.handleMessage(environment).and(safeMessageInfo).contextWrite(context);
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null || !event.isContentChanged()){
            return Mono.empty();
        }

        Context context = Context.of(KEY_LOCALE, entityRetriever.getLocale(guildId),
                KEY_TIMEZONE, entityRetriever.getTimeZone(guildId));

        return Mono.zip(event.getMessage(), event.getChannel().ofType(TextChannel.class))
                .filter(predicate((message, channel) -> !message.isTts()))
                .zipWhen(tuple -> tuple.getT1().getAuthorAsMember(),
                        (tuple, user) -> Tuples.of(tuple.getT1(), tuple.getT2(), user))
                .filter(predicate((message, channel, member) -> DiscordUtil.isNotBot(member)))
                .flatMap(function((message, channel, member) -> {
                    String newContent = MessageUtil.effectiveContent(message);
                    MessageInfo info = messageService.getById(event.getMessageId());
                    if(info == null){
                        info = new MessageInfo();
                        info.messageId(event.getMessageId());
                        info.userId(member.getId());
                        info.guildId(guildId);
                        info.content(messageService.encrypt(event.getOld()
                                .map(MessageUtil::effectiveContent)
                                .orElse(""), message.getId(), message.getChannelId()));
                        info.timestamp(new DateTime(event.getMessageId().getTimestamp().toEpochMilli()));
                    }

                    String oldContent = messageService.decrypt(info.content(), message.getId(), message.getChannelId());
                    info.content(messageService.encrypt(newContent, message.getId(), message.getChannelId()));
                    messageService.save(info);

                    if(newContent.equals(oldContent)){ // message was pinned
                        return Mono.empty();
                    }

                    Mono<?> command = Mono.defer(() -> {
                        if(messageService.isAwaitEdit(message.getId())){
                            CommandEnvironment environment = CommandEnvironment.builder()
                                    .localMember(entityRetriever.getMember(member))
                                    .message(message)
                                    .member(member)
                                    .context(context)
                                    .build();

                            return commandHandler.handleMessage(environment);
                        }
                        return Mono.empty();
                    });

                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_EDIT)
                            .withChannel(channel)
                            .withUser(member)
                            .withAttribute(OLD_CONTENT, oldContent)
                            .withAttribute(NEW_CONTENT, newContent)
                            .withAttribute(AVATAR_URL, member.getAvatarUrl())
                            .withAttribute(MESSAGE_ID, message.getId());

                    if(newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH){
                        ReusableByteInputStream input = new ReusableByteInputStream();
                        input.withString(String.format("%s%n%s%n%n%s%n%s",
                                messageService.get(context, "audit.message.old-content.title"), oldContent,
                                messageService.get(context, "audit.message.new-content.title"), newContent
                        ));
                        builder.withAttachment(MESSAGE_TXT, input);
                    }

                    return builder.save().and(command);
                }))
                .contextWrite(context);
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        Message message = event.getMessage().orElse(null);
        if(message == null || !message.getEmbeds().isEmpty()){
            return Mono.empty();
        }

        User author = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(author) || guildId == null){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(message.getId());
        if(info == null){
            return Mono.empty();
        }

        Context context = Context.of(KEY_LOCALE, entityRetriever.getLocale(guildId),
                KEY_TIMEZONE, entityRetriever.getTimeZone(guildId));

        return event.getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> {
                    String decrypted = messageService.decrypt(info.content(), message.getId(), message.getChannelId());
                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_DELETE)
                            .withChannel(channel)
                            .withAttribute(OLD_CONTENT, decrypted);

                    if(decrypted.length() >= Field.MAX_VALUE_LENGTH){
                        ReusableByteInputStream input = new ReusableByteInputStream();
                        input.withString(String.format("%s%n%s",
                                messageService.get(context, "audit.message.deleted-content.title"), decrypted
                        ));
                        builder.withAttachment(MESSAGE_TXT, input);
                    }

                    messageService.delete(info);
                    Mono<User> responsibleUser = event.getGuild()
                            .flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MESSAGE_DELETE)))
                            .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)) &&
                                    entry.getTargetId().map(id -> id.equals(info.userId())).orElse(false) &&
                                    entry.getData().options().toOptional()
                                            .map(AuditEntryInfoData::channelId)
                                            .flatMap(Possible::toOptional)
                                            .map(Snowflake::of)
                                            .map(id -> id.equals(message.getChannelId())).orElse(false))
                            .next()
                            .flatMap(entry -> event.getClient().getUserById(entry.getResponsibleUserId()));
                    return responsibleUser.defaultIfEmpty(author).map(user -> builder.withUser(user)
                            .withAttribute(AVATAR_URL, user.getAvatarUrl()))
                            .flatMap(AuditActionBuilder::save);
                })
                .contextWrite(context);
    }
}
