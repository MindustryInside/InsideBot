package inside.data;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.EmojiData;
import inside.Configuration;
import inside.data.entity.*;
import io.r2dbc.postgresql.codec.Interval;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EntityRetrieverImpl implements EntityRetriever {

    private static final List<EmojiData> defaultStarsEmojis = Arrays.asList(
            EmojiData.builder().name("\u2B50").build(),
            EmojiData.builder().name("\uD83C\uDF1F").build(),
            EmojiData.builder().name("\uD83D\uDCAB").build()
    );

    private final Configuration configuration;
    private final RepositoryHolder repositoryHolder;

    public EntityRetrieverImpl(Configuration configuration, RepositoryHolder repositoryHolder) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.repositoryHolder = Objects.requireNonNull(repositoryHolder, "repositoryHolder");
    }

    @Override
    public Mono<ImmutableGuildConfig> getGuildConfigById(Snowflake guildId) {
        return repositoryHolder.guildConfigRepository.findByGuildId(guildId.asLong())
                .cast(ImmutableGuildConfig.class);
    }

    @Override
    public Mono<ImmutableGuildConfig> save(GuildConfig guildConfig) {
        return repositoryHolder.guildConfigRepository.save(guildConfig)
                .cast(ImmutableGuildConfig.class);
    }

    @Override
    public Flux<ImmutableActivity> getAllActivityInGuild(Snowflake guildId) {
        return repositoryHolder.activityRepository.findAllByGuildId(guildId.asLong())
                .cast(ImmutableActivity.class);
    }

    @Override
    public Mono<Tuple2<Long, ImmutableActivity>> getPositionAndActivityById(Snowflake guildId, Snowflake userId) {
        return repositoryHolder.activityRepository.findActivityPositionById(guildId.asLong(), userId.asLong())
                .zipWith(getActivityById(guildId, userId));
    }

    @Override
    public Mono<ImmutableActivity> getActivityById(Snowflake guildId, Snowflake userId) {
        return repositoryHolder.activityRepository.findByGuildIdAndUserId(guildId.asLong(), userId.asLong())
                .cast(ImmutableActivity.class);
    }

    @Override
    public Mono<Long> activityCountInGuild(Snowflake guildId) {
        return repositoryHolder.activityRepository.countByGuildId(guildId.asLong());
    }

    @Override
    public Mono<ImmutableActivity> save(Activity activity) {
        return repositoryHolder.activityRepository.save(activity)
                .cast(ImmutableActivity.class);
    }

    @Override
    public Mono<ImmutableActivityConfig> getActivityConfigById(Snowflake guildId) {
        return repositoryHolder.activityConfigRepository.findByGuildId(guildId.asLong())
                .cast(ImmutableActivityConfig.class);
    }

    @Override
    public Flux<ImmutableActivityConfig> getAllEnabledActivityConfig() {
        return repositoryHolder.activityConfigRepository.findAllEnabled(true)
                .cast(ImmutableActivityConfig.class);
    }

    @Override
    public Mono<ImmutableActivityConfig> save(ActivityConfig activityConfig) {
        return repositoryHolder.activityConfigRepository.save(activityConfig)
                .cast(ImmutableActivityConfig.class);
    }

    @Override
    public Mono<ImmutableReactionRole> getReactionRoleById(Snowflake guildId, Snowflake messageId, Snowflake roleId) {
        return repositoryHolder.reactionRoleRepository.findByGuildIdAndMessageIdAndRoleId(
                guildId.asLong(), messageId.asLong(), roleId.asLong())
                .cast(ImmutableReactionRole.class);
    }

    @Override
    public Flux<ImmutableReactionRole> getAllReactionRolesById(Snowflake guildId, Snowflake messageId) {
        return repositoryHolder.reactionRoleRepository.findAllByGuildIdAndMessageId(guildId.asLong(), messageId.asLong())
                .cast(ImmutableReactionRole.class);
    }

    @Override
    public Mono<Long> reactionRolesCountById(Snowflake guildId, Snowflake messageId) {
        return repositoryHolder.reactionRoleRepository.countByGuildIdAndMessageId(guildId.asLong(), messageId.asLong());
    }

    @Override
    public Mono<Void> deleteAllReactionRolesById(Snowflake guildId, Snowflake messageId) {
        return repositoryHolder.reactionRoleRepository
                .deleteByGuildIdAndMessageId(guildId.asLong(), messageId.asLong()).then();
    }

    @Override
    public Mono<ImmutableReactionRole> save(ReactionRole reactionRole) {
        return repositoryHolder.reactionRoleRepository.save(reactionRole)
                .cast(ImmutableReactionRole.class);
    }

    @Override
    public Mono<Void> delete(ReactionRole reactionRole) {
        return repositoryHolder.reactionRoleRepository.delete(reactionRole).then();
    }

    @Override
    public Mono<ImmutableStarboard> getStarboardById(Snowflake guildId, Snowflake sourceMessageId) {
        return repositoryHolder.starboardRepository.findByGuildIdAndSourceMessageId(guildId.asLong(), sourceMessageId.asLong())
                .cast(ImmutableStarboard.class);
    }

    @Override
    public Mono<Void> deleteStarboardBySourceId(Snowflake guildId, Snowflake sourceMessageId) {
        return repositoryHolder.starboardRepository
                .deleteByGuildIdAndSourceMessageId(guildId.asLong(), sourceMessageId.asLong())
                .then();
    }

    @Override
    public Mono<ImmutableStarboard> save(Starboard starboard) {
        return repositoryHolder.starboardRepository.save(starboard)
                .cast(ImmutableStarboard.class);
    }

    @Override
    public Mono<Void> delete(Starboard starboard) {
        return repositoryHolder.starboardRepository.delete(starboard).then();
    }

    @Override
    public Mono<ImmutableStarboardConfig> getStarboardConfigById(Snowflake guildId) {
        return repositoryHolder.starboardConfigRepository.findByGuildId(guildId.asLong())
                .cast(ImmutableStarboardConfig.class);
    }

    @Override
    public Mono<ImmutableStarboardConfig> save(StarboardConfig starboardConfig) {
        return repositoryHolder.starboardConfigRepository.save(starboardConfig)
                .cast(ImmutableStarboardConfig.class);
    }

    @Override
    public Mono<ImmutableGuildConfig> createGuildConfig(Snowflake guildId) {
        return save(GuildConfig.builder()
                .guildId(guildId.asLong())
                .locale(configuration.discord().locale())
                .timezone(configuration.discord().timezone())
                .build());
    }

    @Override
    public Mono<ImmutableActivity> createActivity(Snowflake guildId, Snowflake userId) {
        return save(Activity.builder()
                .guildId(guildId.asLong())
                .userId(userId.asLong())
                .messageCount(0)
                .build());
    }

    @Override
    public Mono<ImmutableActivityConfig> createActivityConfig(Snowflake guildId) {
        return save(ActivityConfig.builder()
                .guildId(guildId.asLong())
                .enabled(false)
                // просто значения-заглушки; всё равно enabled == false
                .countingInterval(Interval.ZERO)
                .messageThreshold(-1)
                .roleId(-1)
                .build());
    }

    @Override
    public Mono<ImmutableStarboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId) {
        return save(Starboard.builder()
                .guildId(guildId.asLong())
                .sourceMessageId(sourceMessageId.asLong())
                .targetMessageId(targetMessageId.asLong())
                .build());
    }

    @Override
    public Mono<ImmutableStarboardConfig> createStarboardConfig(Snowflake guildId) {
        return save(StarboardConfig.builder()
                .guildId(guildId.asLong())
                .enabled(false)
                .threshold(-1)
                .starboardChannelId(-1)
                .selfStarring(false)
                .emojis(defaultStarsEmojis)
                .build());
    }
}
