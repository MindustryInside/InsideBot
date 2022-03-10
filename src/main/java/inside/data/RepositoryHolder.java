package inside.data;

import inside.data.api.RepositoryFactory;
import inside.data.repository.*;

public final class RepositoryHolder {
    public final GuildConfigRepository guildConfigRepository;
    public final ActivityRepository activityRepository;
    public final ActivityConfigRepository activityConfigRepository;
    public final ReactionRoleRepository reactionRoleRepository;
    public final StarboardRepository starboardRepository;
    public final StarboardConfigRepository starboardConfigRepository;
    public final ModerationActionRepository moderationActionRepository;
    public final ModerationConfigRepository moderationConfigRepository;

    public RepositoryHolder(RepositoryFactory factory) {
        guildConfigRepository = factory.create(GuildConfigRepository.class);
        activityRepository = factory.create(ActivityRepository.class);
        activityConfigRepository = factory.create(ActivityConfigRepository.class);
        reactionRoleRepository = factory.create(ReactionRoleRepository.class);
        starboardRepository = factory.create(StarboardRepository.class);
        starboardConfigRepository = factory.create(StarboardConfigRepository.class);
        moderationActionRepository = factory.create(ModerationActionRepository.class);
        moderationConfigRepository = factory.create(ModerationConfigRepository.class);
    }
}
