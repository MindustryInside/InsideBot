package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.AuditLogQuerySpec;
import inside.audit.*;
import inside.command.CommandHandler;
import inside.command.model.CommandEnvironment;
import inside.data.entity.MessageInfo;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import inside.util.io.ReusableByteInputStream;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.util.context.Context;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.function.Predicate;

import static inside.audit.Attribute.*;
import static inside.audit.AuditActionType.*;
import static inside.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.event.MemberEventHandler.TIMEOUT_MILLIS;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

@Component
public class MessageEventHandler extends ReactiveEventAdapter{
    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private MessageService messageService;

    @Lazy
    @Autowired
    private AuditService auditService;

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        if(DiscordUtil.isBot(member) || MessageUtil.isEmpty(message) || message.isTts()){
            return Mono.empty();
        }

        if(message.getType() != Message.Type.REPLY && message.getType() != Message.Type.DEFAULT){
            return Mono.empty();
        }

        Snowflake guildId = member.getGuildId();

        Mono<Void> updateActivity = entityRetriever.getAndUpdateLocalMemberById(member)
                .switchIfEmpty(entityRetriever.createLocalMember(member))
                .flatMap(localMember0 -> {
                    localMember0.getActivity().setLastSentMessage(message.getTimestamp());
                    localMember0.getActivity().incrementMessageCount();
                    return entityRetriever.save(localMember0);
                });

        Mono<Void> safeMessageInfo = entityRetriever.getAuditConfigById(guildId).flatMap(auditConfig -> {
            if(auditConfig.isEnabled(MESSAGE_CREATE)){
                return entityRetriever.createMessageInfo(message).then();
            }
            return Mono.empty();
        });

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<Void> handleMessage = message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> Mono.deferContextual(ctx ->
                        commandHandler.handleMessage(CommandEnvironment.builder()
                                .member(member)
                                .message(message)
                                .channel(channel)
                                .context(ctx)
                                .build())));

        return initContext.flatMap(context -> Mono.when(handleMessage, updateActivity, safeMessageInfo).contextWrite(context));
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null || !event.isContentChanged()){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<Message> updatedMessage = event.getMessage()
                .filter(Predicate.not(Message::isTts));

        Mono<GuildMessageChannel> messageChannel = event.getChannel()
                .ofType(GuildMessageChannel.class);

        Mono<Member> author = updatedMessage.flatMap(Message::getAuthorAsMember)
                .filter(Predicate.not(DiscordUtil::isBot));

        return initContext.flatMap(context -> Mono.zip(updatedMessage, messageChannel, author)
                .flatMap(function((message, channel, member) -> {
                    String newContent = MessageUtil.effectiveContent(message);

                    Mono<MessageInfo> messageInfo = entityRetriever.getMessageInfoById(event.getMessageId())
                            .switchIfEmpty(Mono.fromSupplier(() -> { // create if not stored
                                MessageInfo info = new MessageInfo();
                                info.setMessageId(event.getMessageId());
                                info.setUserId(member.getId());
                                info.setGuildId(guildId);
                                info.setContent(messageService.encrypt(event.getOld()
                                        .map(MessageUtil::effectiveContent)
                                        .orElse(""), message.getId(), message.getChannelId()));
                                info.setTimestamp(event.getMessageId().getTimestamp());
                                return info;
                            }));

                    Mono<?> command = Mono.defer(() -> {
                        if(messageService.isAwaitEdit(message.getId())){
                            return commandHandler.handleMessage(CommandEnvironment.builder()
                                    .channel(channel)
                                    .message(message)
                                    .member(member)
                                    .context(context)
                                    .build());
                        }
                        return Mono.empty();
                    });

                    return messageInfo.flatMap(info -> {
                        String oldContent = messageService.decrypt(info.getContent(), message.getId(), message.getChannelId());
                        info.setContent(messageService.encrypt(newContent, message.getId(), message.getChannelId()));

                        if(newContent.equals(oldContent)){ // message was pinned
                            return Mono.empty();
                        }

                        AuditActionBuilder builder = auditService.newBuilder(guildId, MESSAGE_EDIT)
                                .withChannel(channel)
                                .withUser(member)
                                .withAttribute(MESSAGE, message)
                                .withAttribute(OLD_CONTENT, oldContent)
                                .withAttribute(NEW_CONTENT, newContent)
                                .withAttribute(AVATAR_URL, member.getAvatarUrl())
                                .withAttribute(MESSAGE_ID, message.getId());

                        if(newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH){
                            builder.withAttachment(MESSAGE_TXT, ReusableByteInputStream.ofString(String.format("%s%n%s%n%n%s%n%s",
                                    messageService.get(context, "audit.message.old-content.title"), oldContent,
                                    messageService.get(context, "audit.message.new-content.title"), newContent
                            )));
                        }

                        return builder.save().and(entityRetriever.save(info));
                    }).and(command);
                }))
                .contextWrite(context));
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

        Mono<MessageInfo> messageInfo = entityRetriever.getMessageInfoById(message.getId());

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<Void> logMessageDelete = initContext.flatMap(context ->
                Mono.zip(event.getChannel().ofType(GuildMessageChannel.class), messageInfo)
                .flatMap(function((channel, info) -> {
                    String decrypted = messageService.decrypt(info.getContent(), message.getId(), message.getChannelId());
                    AuditActionBuilder builder = auditService.newBuilder(guildId, MESSAGE_DELETE)
                            .withChannel(channel)
                            .withAttribute(OLD_CONTENT, decrypted);

                    if(decrypted.length() >= Field.MAX_VALUE_LENGTH){
                        builder.withAttachment(MESSAGE_TXT, ReusableByteInputStream.ofString(String.format("%s%n%s",
                                messageService.get(context, "audit.message.deleted-content.title"), decrypted
                        )));
                    }

                    Mono<User> responsibleUser = event.getGuild()
                            .flatMapMany(guild -> guild.getAuditLog(AuditLogQuerySpec.builder()
                                    .actionType(ActionType.MESSAGE_DELETE)
                                    .build()))
                            .flatMap(part -> Flux.fromIterable(part.getEntries()))
                            .sort(Comparator.comparing(AuditLogEntry::getId).reversed())
                            .filter(entry -> entry.getId().getTimestamp()
                                    .isAfter(Instant.now(Clock.systemUTC()).minusMillis(TIMEOUT_MILLIS)) ||
                                    entry.getOption(OptionKey.COUNT).map(i -> i > 1).orElse(false) &&
                                            entry.getId().getTimestamp().isAfter(Instant.now(Clock.systemUTC())
                                                    .minus(5, ChronoUnit.MINUTES)))
                            .filter(entry -> entry.getTargetId().map(id -> id.equals(info.getUserId())).orElse(false) &&
                                    entry.getOption(OptionKey.CHANNEL_ID).map(id -> id.equals(message.getChannelId())).orElse(false))
                            .next()
                            .flatMap(entry -> Mono.justOrEmpty(entry.getResponsibleUser()));

                    return responsibleUser.defaultIfEmpty(author).map(user -> builder.withUser(user)
                                    .withTargetUser(author)
                                    .withAttribute(MESSAGE, message)
                                    .withAttribute(AVATAR_URL, author.getAvatarUrl()))
                            .flatMap(AuditActionBuilder::save)
                            .and(entityRetriever.delete(info));
                }))
                .contextWrite(context));

        Mono<Void> deletePoll = entityRetriever.getPollById(message.getId())
                .flatMap(entityRetriever::delete);

        return Mono.when(logMessageDelete, deletePoll);
    }
}
