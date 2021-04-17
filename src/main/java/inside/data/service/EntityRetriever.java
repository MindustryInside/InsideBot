package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import reactor.core.publisher.Mono;

public interface EntityRetriever{

    // guild config

    Mono<GuildConfig> getGuildConfigById(Snowflake guildId);

    Mono<Void> save(GuildConfig guildConfig);

    // admin config

    Mono<AdminConfig> getAdminConfigById(Snowflake guildId);

    Mono<Void> save(AdminConfig adminConfig);

    // audit config

    Mono<AuditConfig> getAuditConfigById(Snowflake guildId);

    Mono<Void> save(AuditConfig auditConfig);

    // member

    Mono<LocalMember> getLocalMemberById(Snowflake userId, Snowflake guildId);

    default Mono<LocalMember> getLocalMemberById(Member member){
        return getLocalMemberById(member.getId(), member.getGuildId());
    }

    Mono<Void> save(LocalMember localMember);

    Mono<GuildConfig> createGuildConfig(Snowflake guildId);

    Mono<AdminConfig> createAdminConfig(Snowflake guildId);

    Mono<AuditConfig> createAuditConfig(Snowflake guildId);

    Mono<LocalMember> createLocalMember(Snowflake userId, Snowflake guildId, String effectiveNickname);

    default Mono<LocalMember> createLocalMember(Member member){
        return createLocalMember(member.getId(), member.getGuildId(), member.getDisplayName());
    }
}
