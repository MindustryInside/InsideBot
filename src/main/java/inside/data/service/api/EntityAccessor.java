package inside.data.service.api;

import inside.data.entity.*;
import reactor.core.publisher.*;

public interface EntityAccessor{

    Mono<GuildConfig> getGuildConfigById(long guildId);

    Mono<AdminConfig> getAdminConfigById(long guildId);

    Mono<AuditConfig> getAuditConfigById(long guildId);

    Flux<LocalMember> getAllLocalMembers();

    Mono<LocalMember> getLocalMemberById(long userId, long guildId);

    Mono<MessageInfo> getMessageInfoById(long messageId);

    Mono<StarboardConfig> getStarboardConfigById(long guildId);

    Mono<Starboard> getStarboardById(long guildId, long sourceMessageId);

    Mono<ActiveUserConfig> getActiveUserConfigById(long guildId);
}
