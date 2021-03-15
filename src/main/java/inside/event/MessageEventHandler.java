package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import inside.command.CommandHandler;
import inside.command.model.CommandReference;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.audit.*;
import inside.util.*;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.*;
import reactor.util.context.Context;
import reactor.util.function.Tuples;

import static inside.event.audit.Attribute.*;
import static inside.event.audit.AuditActionType.*;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

@Component
public class MessageEventHandler extends ReactiveEventAdapter{
    private static final Logger log = Loggers.getLogger(MessageEventHandler.class);

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
        if(member == null || MessageUtil.isEmpty(message) || message.getType() != Message.Type.DEFAULT || message.isTts() || !message.getEmbeds().isEmpty()){
            return Mono.empty();
        }

        Mono<MessageChannel> channel = message.getChannel();
        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null){
            return Mono.empty();
        }

        Snowflake userId = user.getId();
        LocalMember localMember = entityRetriever.getMember(member);

        localMember.lastSentMessage(DateTime.now());
        entityRetriever.save(localMember);

        Mono<Void> messageInfo = Mono.fromRunnable(() -> {
            MessageInfo info = new MessageInfo();
            info.userId(userId);
            info.messageId(message.getId());
            info.guildId(guildId);
            info.timestamp(DateTime.now());
            info.content(MessageUtil.effectiveContent(message));
            messageService.save(info);
        });

        Context context = Context.of(KEY_LOCALE, entityRetriever.locale(guildId),
                KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        CommandReference reference = CommandReference.builder()
                .message(message)
                .member(member)
                .context(context)
                .localMember(localMember)
                .channel(() -> channel)
                .build();

        return commandHandler.handleMessage(reference).and(messageInfo).contextWrite(context);
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null || !event.isContentChanged()){
            return Mono.empty();
        }

        Context context = Context.of(KEY_LOCALE, entityRetriever.locale(guildId),
                KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        return Mono.zip(event.getMessage(), event.getChannel().ofType(TextChannel.class))
                .filter(predicate((message, channel) -> !message.isTts() && !message.isPinned()))
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
                        info.content(event.getOld().map(MessageUtil::effectiveContent).orElse(""));
                        info.timestamp(new DateTime(event.getMessageId().getTimestamp().toEpochMilli()));
                    }
                    String oldContent = info.content();
                    info.content(newContent);
                    messageService.save(info);

                    Mono<?> command = Mono.defer(() -> {
                        if(messageService.isAwaitEdit(message.getId())){
                            CommandReference reference = CommandReference.builder()
                                    .localMember(entityRetriever.getMember(member))
                                    .message(message)
                                    .member(member)
                                    .context(context)
                                    .build();

                            return commandHandler.handleMessage(reference);
                        }
                        return Mono.empty();
                    });

                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_EDIT)
                            .withChannel(channel)
                            .withUser(member)
                            .withAttribute(OLD_CONTENT, oldContent)
                            .withAttribute(NEW_CONTENT, newContent)
                            .withAttribute(USER_URL, member.getAvatarUrl())
                            .withAttribute(MESSAGE_ID, message.getId());

                    if(newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH){
                        StringInputStream input = new StringInputStream();
                        input.writeString(String.format("%s:%n%s%n%n%s:%n%s",
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
        if(message == null){
            return Mono.empty();
        }

        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null || !messageService.exists(message.getId())){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(message.getId());
        String content = info.content();

        Context context = Context.of(KEY_LOCALE, entityRetriever.locale(guildId),
                KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        return event.getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> {
                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_DELETE)
                            .withChannel(channel)
                            .withUser(user)
                            .withAttribute(USER_URL, user.getAvatarUrl())
                            .withAttribute(OLD_CONTENT, content);

                    if(content.length() >= Field.MAX_VALUE_LENGTH){
                        StringInputStream input = new StringInputStream();
                        input.writeString(String.format("%s:%n%s",
                                messageService.get(context, "audit.message.deleted-content.title"), content
                        ));
                        builder.withAttachment(MESSAGE_TXT, input);
                    }

                    messageService.delete(info);
                    return builder.save();
                })
                .contextWrite(context);
    }
}
