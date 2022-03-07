package inside.data;

import discord4j.common.util.Snowflake;
import inside.data.entity.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EntityRetriever {

    //region guild config

    Mono<ImmutableGuildConfig> getGuildConfigById(Snowflake guildId);

    Mono<ImmutableGuildConfig> save(GuildConfig guildConfig);

    //endregion
    //region activity

    Flux<ImmutableActivity> getAllActivityInGuild(Snowflake guildId);

    Mono<ImmutableActivity> getActivityById(Snowflake guildId, Snowflake userId);

    Mono<ImmutableActivity> save(Activity activity);

    //endregion
    //region activity config

    Mono<ImmutableActivityConfig> getActivityConfigById(Snowflake guildId);

    Flux<ImmutableActivityConfig> getAllEnabledActivityConfig();

    Mono<ImmutableActivityConfig> save(ActivityConfig activityConfig);

    //endregion
    //region reaction role

    Mono<ImmutableReactionRole> getReactionRoleById(Snowflake guildId, Snowflake messageId, Snowflake roleId);

    Flux<ImmutableReactionRole> getAllReactionRolesById(Snowflake guildId, Snowflake messageId);

    Mono<Long> reactionRolesCountById(Snowflake guildId, Snowflake messageId);

    Mono<Void> deleteAllReactionRolesById(Snowflake guildId, Snowflake messageId);

    Mono<ImmutableReactionRole> save(ReactionRole reactionRole);

    Mono<Void> delete(ReactionRole reactionRole);

    //endregion
    //region starboard

    Mono<ImmutableStarboard> getStarboardById(Snowflake guildId, Snowflake sourceMessageId);

    Mono<Void> deleteStarboardBySourceId(Snowflake guildId, Snowflake sourceMessageId);

    Mono<ImmutableStarboard> save(Starboard starboard);

    Mono<Void> delete(Starboard starboard);

    //endregion
    //region starboard config

    Mono<ImmutableStarboardConfig> getStarboardConfigById(Snowflake guildId);

    Mono<ImmutableStarboardConfig> save(StarboardConfig starboardConfig);

    //endregion
    //region factory methods
    // Возможно и не нужно

    Mono<ImmutableGuildConfig> createGuildConfig(Snowflake guildId);

    Mono<ImmutableActivity> createActivity(Snowflake guildId, Snowflake userId);

    Mono<ImmutableActivityConfig> createActivityConfig(Snowflake guildId);

    Mono<ImmutableStarboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId);

    Mono<ImmutableStarboardConfig> createStarboardConfig(Snowflake guildId);

    //endregion
}
