package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import inside.data.EntityRetriever;
import inside.data.entity.base.ConfigEntity;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

import static reactor.function.TupleUtils.function;

public class MessageEventHandler extends ReactiveEventAdapter {

    private final EntityRetriever entityRetriever;

    public MessageEventHandler(EntityRetriever entityRetriever) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        if(member == null || member.isBot() || message.getContent().isEmpty() ||
                message.isTts() || message.getType() != Message.Type.DEFAULT){
            return Mono.empty();
        }

        return entityRetriever.getActivityConfigById(member.getGuildId())
                .filter(ConfigEntity::enabled)
                .zipWith(entityRetriever.getActivityById(member.getGuildId(), member.getId())
                        .switchIfEmpty(entityRetriever.createActivity(member.getGuildId(), member.getId()))
                        .map(ac -> ac.incrementMessageCount()
                                .withLastSentMessage(message.getTimestamp())))
                .flatMap(function((config, activity) -> Mono.defer(() -> {
                    Snowflake roleId = Snowflake.of(config.roleId());
                    if (activity.messageCount() >= config.messageThreshold() && activity.lastSentMessage()
                            .map(i -> i.isAfter(Instant.now().minus(config.countingInterval())))
                            .orElse(false)) {
                        if (member.getRoleIds().contains(roleId)) {
                            return Mono.just(activity);
                        }

                        return member.addRole(roleId)
                                .thenReturn(activity);
                    }

                    return Mono.just(activity);
                })
                .then(entityRetriever.save(activity))));
    }
}
