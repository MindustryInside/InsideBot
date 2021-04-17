package inside.data.service.api;

import inside.data.entity.*;
import reactor.core.publisher.Mono;

public interface EntityAccessor{

    Mono<GuildConfig> getGuildConfigById(long guildId);

    Mono<AdminConfig> getAdminConfigById(long guildId);

    Mono<AuditConfig> getAuditConfigById(long guildId);

    Mono<LocalMember> getLocalMemberById(long userId, long guildId);

    Mono<MessageInfo> getMessageInfoById(long messageId);
}
