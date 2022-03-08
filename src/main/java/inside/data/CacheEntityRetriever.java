package inside.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import inside.data.entity.*;
import inside.data.entity.base.ConfigEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.Objects;

public class CacheEntityRetriever implements EntityRetriever {
    private final EntityRetriever delegate;
    private final Cache<Long, ImmutableGuildConfig> guildConfigs = Caffeine.newBuilder().build();
    private final Cache<Tuple2<Long, Long>, ImmutableActivity> activities = Caffeine.newBuilder().build();
    private final Cache<Long, ImmutableActivityConfig> activityConfigs = Caffeine.newBuilder().build();
    private final Cache<Tuple3<Long, Long, Long>, ImmutableReactionRole> reactionRoles = Caffeine.newBuilder().build();
    private final Cache<Tuple2<Long, Long>, ImmutableStarboard> starboards = Caffeine.newBuilder().build();
    private final Cache<Long, ImmutableStarboardConfig> starboardConfigs = Caffeine.newBuilder().build();

    public CacheEntityRetriever(EntityRetriever delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Mono<ImmutableGuildConfig> getGuildConfigById(Snowflake guildId) {
        return Mono.fromSupplier(() -> guildConfigs.getIfPresent(guildId.asLong()))
                .switchIfEmpty(delegate.getGuildConfigById(guildId)
                        // На всякий случай запишем в кеш
                        .doOnNext(v -> guildConfigs.put(v.guildId(), v)));
    }

    @Override
    public Mono<ImmutableGuildConfig> save(GuildConfig guildConfig) {
        return delegate.save(guildConfig)
                // Записываем в кеш только тогда, когда идентификатор присвоен
                .doOnNext(v -> guildConfigs.put(v.guildId(), v));
    }

    @Override
    public Flux<ImmutableActivity> getAllActivityInGuild(Snowflake guildId) {
        return Flux.fromStream(activities.asMap().keySet().stream()
                .filter(k -> k.getT1() == guildId.asLong())
                .map(activities::getIfPresent)
                .filter(Objects::nonNull))
                .switchIfEmpty(delegate.getAllActivityInGuild(guildId)
                        .doOnNext(v -> activities.put(Tuples.of(v.guildId(), v.userId()), v)));
    }

    @Override
    public Mono<Tuple2<Long, ImmutableActivity>> getPositionAndActivityById(Snowflake guildId, Snowflake userId) {
        return delegate.getPositionAndActivityById(guildId, userId);
    }

    @Override
    public Mono<ImmutableActivity> getActivityById(Snowflake guildId, Snowflake userId) {
        return Mono.fromSupplier(() -> activities.getIfPresent(Tuples.of(guildId.asLong(), userId.asLong())))
                .switchIfEmpty(delegate.getActivityById(guildId, userId)
                        .doOnNext(v -> activities.put(Tuples.of(v.guildId(), v.userId()), v)));
    }

    @Override
    public Mono<Long> activityCountInGuild(Snowflake guildId) {
        return delegate.activityCountInGuild(guildId);
    }

    @Override
    public Mono<ImmutableActivity> save(Activity activity) {
        return delegate.save(activity)
                .doOnNext(v -> activities.put(Tuples.of(v.guildId(), v.userId()), v));
    }

    @Override
    public Mono<ImmutableActivityConfig> getActivityConfigById(Snowflake guildId) {
        return Mono.fromSupplier(() -> activityConfigs.getIfPresent(guildId.asLong()))
                .switchIfEmpty(delegate.getActivityConfigById(guildId)
                        .doOnNext(v -> activityConfigs.put(guildId.asLong(), v)));
    }

    @Override
    public Flux<ImmutableActivityConfig> getAllEnabledActivityConfig() {
        return Flux.fromStream(activityConfigs.asMap().values().stream()
                .filter(ConfigEntity::enabled)
                .map(activityConfigs::getIfPresent)
                .filter(Objects::nonNull))
                .switchIfEmpty(delegate.getAllEnabledActivityConfig()
                        .doOnNext(v -> activityConfigs.put(v.guildId(), v)));
    }

    @Override
    public Mono<ImmutableActivityConfig> save(ActivityConfig activityConfig) {
        return delegate.save(activityConfig)
                .doOnNext(v -> activityConfigs.put(v.guildId(), v));
    }

    @Override
    public Mono<ImmutableReactionRole> getReactionRoleById(Snowflake guildId, Snowflake messageId, Snowflake roleId) {
        return Mono.fromSupplier(() -> reactionRoles.getIfPresent(Tuples.of(guildId.asLong(), messageId.asLong(), roleId.asLong())))
                .switchIfEmpty(delegate.getReactionRoleById(guildId, messageId, roleId)
                        .doOnNext(v -> reactionRoles.put(Tuples.of(v.guildId(), v.messageId(), v.roleId()), v)));
    }

    @Override
    public Flux<ImmutableReactionRole> getAllReactionRolesById(Snowflake guildId, Snowflake messageId) {
        return Flux.fromStream(reactionRoles.asMap().values().stream()
                .filter(v -> v.guildId() == guildId.asLong() && v.messageId() == messageId.asLong())
                .map(reactionRoles::getIfPresent)
                .filter(Objects::nonNull))
                .switchIfEmpty(delegate.getAllReactionRolesById(guildId, messageId)
                        .doOnNext(v -> reactionRoles.put(Tuples.of(v.guildId(), v.messageId(), v.roleId()), v)));
    }

    @Override
    public Mono<Long> reactionRolesCountById(Snowflake guildId, Snowflake messageId) {
        return delegate.reactionRolesCountById(guildId, messageId);
    }

    @Override
    public Mono<Void> deleteAllReactionRolesById(Snowflake guildId, Snowflake messageId) {
        return delegate.deleteAllReactionRolesById(guildId, messageId)
                .and(Mono.fromRunnable(reactionRoles.asMap()::clear));
    }

    @Override
    public Mono<ImmutableReactionRole> save(ReactionRole reactionRole) {
        return delegate.save(reactionRole)
                .doOnNext(v -> reactionRoles.put(Tuples.of(v.guildId(), v.messageId(), v.roleId()), v));
    }

    @Override
    public Mono<Void> delete(ReactionRole reactionRole) {
        return delegate.delete(reactionRole)
                .and(Mono.fromRunnable(() -> reactionRoles.invalidate(Tuples.of(
                        reactionRole.guildId(), reactionRole.messageId(), reactionRole.roleId()))));
    }

    @Override
    public Mono<ImmutableStarboard> getStarboardById(Snowflake guildId, Snowflake sourceMessageId) {
        return Mono.fromSupplier(() -> starboards.getIfPresent(Tuples.of(guildId.asLong(), sourceMessageId.asLong())))
                .switchIfEmpty(delegate.getStarboardById(guildId, sourceMessageId)
                        .doOnNext(v -> starboards.put(Tuples.of(v.guildId(), v.sourceMessageId()), v)));
    }

    @Override
    public Mono<Void> deleteStarboardBySourceId(Snowflake guildId, Snowflake sourceMessageId) {
        return delegate.deleteStarboardBySourceId(guildId, sourceMessageId)
                .and(Mono.fromRunnable(() -> starboards.invalidate(Tuples.of(guildId.asLong(), sourceMessageId.asLong()))));
    }

    @Override
    public Mono<ImmutableStarboard> save(Starboard starboard) {
        return delegate.save(starboard)
                .doOnNext(v -> starboards.put(Tuples.of(v.guildId(), v.sourceMessageId()), v));
    }

    @Override
    public Mono<Void> delete(Starboard starboard) {
        return delegate.delete(starboard)
                .and(Mono.fromRunnable(() -> starboards.invalidate(Tuples.of(
                        starboard.guildId(), starboard.sourceMessageId()))));
    }

    @Override
    public Mono<ImmutableStarboardConfig> getStarboardConfigById(Snowflake guildId) {
        return Mono.fromSupplier(() -> starboardConfigs.getIfPresent(guildId.asLong()))
                .switchIfEmpty(delegate.getStarboardConfigById(guildId)
                        .doOnNext(v -> starboardConfigs.put(v.guildId(), v)));
    }

    @Override
    public Mono<ImmutableStarboardConfig> save(StarboardConfig starboardConfig) {
        return delegate.save(starboardConfig)
                .doOnNext(v -> starboardConfigs.put(v.guildId(), v));
    }

    @Override
    public Mono<ImmutableGuildConfig> createGuildConfig(Snowflake guildId) {
        return delegate.createGuildConfig(guildId);
    }

    @Override
    public Mono<ImmutableActivity> createActivity(Snowflake guildId, Snowflake userId) {
        return delegate.createActivity(guildId, userId);
    }

    @Override
    public Mono<ImmutableActivityConfig> createActivityConfig(Snowflake guildId) {
        return delegate.createActivityConfig(guildId);
    }

    @Override
    public Mono<ImmutableStarboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId) {
        return delegate.createStarboard(guildId, sourceMessageId, targetMessageId);
    }

    @Override
    public Mono<ImmutableStarboardConfig> createStarboardConfig(Snowflake guildId) {
        return delegate.createStarboardConfig(guildId);
    }
}
