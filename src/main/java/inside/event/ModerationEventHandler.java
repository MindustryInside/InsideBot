package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import inside.Launcher;
import inside.data.EntityRetriever;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.util.stream.Stream;

import static inside.util.ContextUtil.KEY_LOCALE;
import static inside.util.ContextUtil.KEY_TIMEZONE;

public class ModerationEventHandler extends ReactiveEventAdapter {

    private final EntityRetriever entityRetriever;

    public ModerationEventHandler(EntityRetriever entityRetriever) {
        this.entityRetriever = entityRetriever;
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        Message.Type messageType = message.getType();
        if (member == null || member.isBot() || message.getContent().isEmpty() || message.isTts() ||
            messageType != Message.Type.DEFAULT && messageType != Message.Type.REPLY) {
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(member.getGuildId())
                .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timezone()));

        return initContext.flatMap(ctx -> entityRetriever.getModerationConfigById(member.getGuildId())
                .filter(config -> config.pingSpamThreshold()
                        .map(t -> Stream.of(message.getReferencedMessage().map(Message::getId),
                                        message.getRoleMentionIds(), message.getUserMentionIds())
                                .distinct()
                                .count() >= t)
                        .orElse(false))
                .flatMap(config -> message.delete()
                        // TODO: кидать в мут
                        .and(message.getChannel()
                                .zipWith(member.getGuild()
                                        .map(Guild::getOwnerId)
                                        .map(MessageUtil::getUserMention))
                                .flatMap(TupleUtils.function((c, o) -> c.createMessage(
                                        "%s, пользователь **%s** (%s) потенциальный пинг-спаммер. Подозрительное сообщение:\n||%s||"
                                                .formatted(o, member.getDisplayName(), member.getMention(),
                                                        message.getContent()))
                                        .withAllowedMentions(Launcher.suppressAll))))
                        .contextWrite(ctx)));
    }
}
