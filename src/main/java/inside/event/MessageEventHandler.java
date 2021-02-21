package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.object.entity.channel.TextChannel;
import inside.common.command.CommandHandler;
import inside.common.command.model.base.CommandReference;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import inside.event.audit.*;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;
import reactor.util.function.Tuples;

import java.util.Calendar;

import static inside.event.audit.AuditEventType.*;
import static inside.event.audit.AuditProviders.MessageEditAuditProvider.KEY_NEW_CONTENT;
import static inside.event.audit.MessageAuditProvider.*;
import static inside.util.ContextUtil.*;

@Component
public class MessageEventHandler extends AuditEventHandler{
    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AuditService auditService;

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        String text = message.getContent();
        Member member = event.getMember().orElse(null);
        if(member == null || message.getType() != Message.Type.DEFAULT || MessageUtil.isEmpty(message) || message.isTts() || !message.getEmbeds().isEmpty()){
            return Mono.empty();
        }

        Mono<TextChannel> channel = message.getChannel().ofType(TextChannel.class);
        User user = message.getAuthor().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(DiscordUtil.isBot(user) || guildId == null){
            return Mono.empty();
        }

        Snowflake userId = user.getId();
        LocalMember localMember = entityRetriever.getMember(member);

        localMember.lastSentMessage(Calendar.getInstance());
        entityRetriever.save(localMember);

        Mono<?> messageInfo = channel.filter(textChannel -> textChannel.getType() == Type.GUILD_TEXT)
                .doOnNext(signal -> {
                    MessageInfo info = new MessageInfo();
                    info.userId(userId);
                    info.messageId(message.getId());
                    info.guildId(guildId);
                    info.timestamp(Calendar.getInstance());
                    info.content(MessageUtil.effectiveContent(message));
                    messageService.save(info);
                });

        Context context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, entityRetriever.locale(guildId),
                             KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        CommandReference reference = CommandReference.builder()
                .event(event)
                .context(context)
                .localMember(localMember)
                .channel(() -> channel)
                .build();

        return messageInfo.flatMap(__ -> commandHandler.handleMessage(text, reference).contextWrite(context));
    }

    @Override
    public Publisher<?> onMessageUpdate(MessageUpdateEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null || !event.isContentChanged() || !messageService.exists(event.getMessageId())){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(event.getMessageId());
        String oldContent = info.content();

        Context context = Context.of(KEY_GUILD_ID, guildId, KEY_LOCALE, entityRetriever.locale(guildId), KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        return Mono.zip(event.getMessage(), event.getChannel().ofType(TextChannel.class))
                .filter(TupleUtils.predicate((message, channel) -> !message.isTts() && !message.isPinned()))
                .zipWhen(tuple -> Mono.justOrEmpty(tuple.getT1().getAuthor()), (tuple, user) -> Tuples.of(tuple.getT1(), tuple.getT2(), user))
                .filter(TupleUtils.predicate((message, channel, user) -> DiscordUtil.isNotBot(user)))
                .flatMap(TupleUtils.function((message, channel, user) -> {
                    String newContent = MessageUtil.effectiveContent(message);
                    info.content(newContent);
                    messageService.save(info);

                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_EDIT)
                            .withChannel(channel)
                            .withUser(user)
                            .withAttribute(KEY_OLD_CONTENT, oldContent)
                            .withAttribute(KEY_NEW_CONTENT, newContent)
                            .withAttribute(KEY_MESSAGE_ID, message.getId());

                    if(newContent.length() >= Field.MAX_VALUE_LENGTH || oldContent.length() >= Field.MAX_VALUE_LENGTH){
                        StringInputStream input = new StringInputStream();
                        input.writeString(String.format("%s:%n%s%n%n%s:%n%s",
                                messageService.get(context, "audit.message.old-content.title"), oldContent,
                                messageService.get(context, "audit.message.new-content.title"), newContent
                        ));
                        builder.withAttachment(KEY_MESSAGE_TXT, input);
                    }

                    return builder.save();
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
        if(DiscordUtil.isBot(user) || guildId == null || !messageService.exists(message.getId()) || messageService.isCleared(message.getId())){
            return Mono.empty();
        }

        MessageInfo info = messageService.getById(message.getId());
        String content = info.content();

        Context context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, entityRetriever.locale(guildId),
                             KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        return event.getChannel().ofType(TextChannel.class)
                .flatMap(channel -> {
                    AuditActionBuilder builder = auditService.log(guildId, MESSAGE_DELETE)
                            .withChannel(channel)
                            .withAttribute(KEY_OLD_CONTENT, content);

                    if(content.length() >= Field.MAX_VALUE_LENGTH){
                        StringInputStream input = new StringInputStream();
                        input.writeString(String.format("%s:%n%s", messageService.get(context, "audit.message.deleted-content.title"), content));
                        builder.withAttachment(KEY_MESSAGE_TXT, input);
                    }

                    messageService.delete(info);
                    return builder.save();
                })
                .contextWrite(context);
    }
}
