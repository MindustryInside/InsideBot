package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
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

    // message info

    Mono<MessageInfo> getMessageInfoById(Snowflake messageId);

    Mono<Void> deleteMessageInfoById(Snowflake messageId);

    Mono<Void> delete(MessageInfo messageInfo);

    Mono<Void> save(MessageInfo messageInfo);

    // starboard config

    Mono<StarboardConfig> getStarboardConfigById(Snowflake guildId);

    Mono<Void> save(StarboardConfig starboardConfig);

    // starboard

    Mono<Starboard> getStarboardById(Snowflake guildId, Snowflake sourceMessageId);

    Mono<Void> deleteStarboardById(Snowflake guildId, Snowflake sourceMessageId);

    Mono<Void> delete(Starboard starboard);

    Mono<Void> save(Starboard starboard);

    // factory methods

    Mono<GuildConfig> createGuildConfig(Snowflake guildId);

    Mono<AdminConfig> createAdminConfig(Snowflake guildId);

    Mono<AuditConfig> createAuditConfig(Snowflake guildId);

    Mono<LocalMember> createLocalMember(Snowflake userId, Snowflake guildId, String effectiveNickname);

    Mono<MessageInfo> createMessageInfo(Message message);

    default Mono<LocalMember> createLocalMember(Member member){
        return createLocalMember(member.getId(), member.getGuildId(), member.getDisplayName());
    }

    Mono<StarboardConfig> createStarboardConfig(Snowflake guildId);

    Mono<Starboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId);
}
