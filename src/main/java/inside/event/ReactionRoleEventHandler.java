package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.discordjson.json.EmojiData;
import inside.data.EntityRetriever;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ReactionRoleEventHandler extends ReactiveEventAdapter {

    private final EntityRetriever entityRetriever;

    public ReactionRoleEventHandler(EntityRetriever entityRetriever) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    public Publisher<?> onReactionAdd(ReactionAddEvent event) {
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        Snowflake messageId = event.getMessageId();
        EmojiData emoji = event.getEmoji().asEmojiData();

        return entityRetriever.getAllReactionRolesById(guildId, messageId)
                .filter(role -> role.emoji().equals(emoji))
                .flatMap(role -> event.getClient().rest()
                        .getGuildService().addGuildMemberRole(guildId.asLong(),
                                event.getUserId().asLong(), role.roleId(), null));
    }

    @Override
    public Publisher<?> onReactionRemove(ReactionRemoveEvent event) {
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        Snowflake messageId = event.getMessageId();
        EmojiData emoji = event.getEmoji().asEmojiData();

        return entityRetriever.getAllReactionRolesById(guildId, messageId)
                .filter(role -> role.emoji().equals(emoji))
                .flatMap(role -> event.getClient().rest()
                        .getGuildService().removeGuildMemberRole(guildId.asLong(),
                                event.getUserId().asLong(), role.roleId(), null));
    }
}
