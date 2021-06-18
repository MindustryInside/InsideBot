package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.discordjson.json.EmojiData;
import inside.data.entity.*;
import reactor.core.publisher.*;

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

    Flux<LocalMember> getAllLocalMembers();

    Mono<LocalMember> getLocalMemberById(Snowflake userId, Snowflake guildId);

    Mono<LocalMember> getAndUpdateLocalMemberById(Member member);

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

    // active user config

    Mono<ActiveUserConfig> getActiveUserConfigById(Snowflake guildId);

    Mono<Void> save(ActiveUserConfig activeUserConfig);

    // emoji dispenser

    Mono<EmojiDispenser> getEmojiDispenserById(Snowflake messageId);

    Mono<Void> delete(EmojiDispenser emojiDispenser);

    Mono<Void> save(EmojiDispenser emojiDispenser);

    // factory methods

    Mono<GuildConfig> createGuildConfig(Snowflake guildId);

    Mono<AdminConfig> createAdminConfig(Snowflake guildId);

    Mono<AuditConfig> createAuditConfig(Snowflake guildId);

    Mono<LocalMember> createLocalMember(Member member);

    Mono<ActiveUserConfig> createActiveUserConfig(Snowflake guildId);

    Mono<MessageInfo> createMessageInfo(Message message);

    Mono<StarboardConfig> createStarboardConfig(Snowflake guildId);

    Mono<Starboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId);

    Mono<EmojiDispenser> createEmojiDispenser(Snowflake guildId, Snowflake messageId, Snowflake roleId, EmojiData emojiData);
}
