package inside.data.service.api;

import inside.data.entity.*;
import reactor.core.publisher.Mono;

public interface EntityUpdater{

    Mono<Void> onGuildConfigSave(GuildConfig guildConfig);

    Mono<Void> onAdminConfigSave(AdminConfig adminConfig);

    Mono<Void> onAuditConfigSave(AuditConfig auditConfig);

    Mono<Void> onLocalMemberSave(LocalMember localMember);
}
