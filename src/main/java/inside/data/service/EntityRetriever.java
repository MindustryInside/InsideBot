package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.discordjson.json.EmojiData;
import inside.data.entity.*;
import reactor.core.publisher.*;

import java.util.List;

public interface EntityRetriever{

    // guild config

    Mono<GuildConfig> getGuildConfigById(Snowflake guildId);

    Mono<Void> save(GuildConfig guildConfig);

    Mono<Void> deleteGuildConfigById(Snowflake guildId);

    // admin config

    Mono<AdminConfig> getAdminConfigById(Snowflake guildId);

    Mono<Void> save(AdminConfig adminConfig);

    Mono<Void> deleteAdminConfigById(Snowflake guildId);

    // audit config

    Mono<AuditConfig> getAuditConfigById(Snowflake guildId);

    Mono<Void> save(AuditConfig auditConfig);

    Mono<Void> deleteAuditConfigById(Snowflake guildId);

    // member

    Flux<LocalMember> getAllLocalMembers();

    Mono<LocalMember> getLocalMemberById(Snowflake userId, Snowflake guildId);

    Mono<LocalMember> getAndUpdateLocalMemberById(Member member);

    Mono<Void> save(LocalMember localMember);

    Mono<Void> deleteAllLocalMembersInGuild(Snowflake guildId);

    // message info

    Mono<MessageInfo> getMessageInfoById(Snowflake messageId);

    Mono<Void> save(MessageInfo messageInfo);

    Mono<Void> delete(MessageInfo messageInfo);

    Mono<Void> deleteMessageInfoById(Snowflake messageId);

    Mono<Void> deleteAllMessageInfoInGuild(Snowflake guildId);

    // starboard config

    Mono<StarboardConfig> getStarboardConfigById(Snowflake guildId);

    Mono<Void> save(StarboardConfig starboardConfig);

    Mono<Void> deleteStarboardConfigById(Snowflake guildId);

    // starboard

    Mono<Starboard> getStarboardBySourceId(Snowflake guildId, Snowflake sourceMessageId);

    Mono<Void> deleteStarboardById(Snowflake guildId, Snowflake sourceMessageId);

    Mono<Void> save(Starboard starboard);

    Mono<Void> delete(Starboard starboard);

    Mono<Void> deleteAllStarboardsInGuild(Snowflake guildId);

    // activity config

    Mono<ActivityConfig> getActivityConfigById(Snowflake guildId);

    Mono<Void> save(ActivityConfig activityConfig);

    Mono<Void> deleteActivityConfigById(Snowflake guildId);

    // emoji dispenser

    Flux<EmojiDispenser> getEmojiDispensersById(Snowflake messageId);

    Mono<EmojiDispenser> getEmojiDispenserById(Snowflake messageId, Snowflake roleId);

    Flux<EmojiDispenser> getAllEmojiDispenserInGuild(Snowflake guildId);

    Mono<Long> getEmojiDispenserCountInGuild(Snowflake guildId);

    Mono<Void> save(EmojiDispenser emojiDispenser);

    Mono<Void> delete(EmojiDispenser emojiDispenser);

    Mono<Void> deleteAllEmojiDispenserInGuild(Snowflake guildId);

    // welcome message

    Mono<WelcomeMessage> getWelcomeMessageById(Snowflake guildId);

    Mono<Void> save(WelcomeMessage welcomeMessage);

    // poll

    Mono<Poll> getPollById(Snowflake messageId);

    Mono<Void> save(Poll poll);

    Mono<Void> delete(Poll poll);

    Mono<Void> deleteAllPollInGuild(Snowflake guildId);

    // command config

    Mono<CommandConfig> getCommandConfigById(Snowflake guildId, String name);

    Mono<Void> save(CommandConfig commandConfig);

    // factory methods

    Mono<GuildConfig> createGuildConfig(Snowflake guildId);

    Mono<AdminConfig> createAdminConfig(Snowflake guildId);

    Mono<AuditConfig> createAuditConfig(Snowflake guildId);

    Mono<LocalMember> createLocalMember(Member member);

    Mono<ActivityConfig> createActiveUserConfig(Snowflake guildId);

    Mono<MessageInfo> createMessageInfo(Message message);

    Mono<StarboardConfig> createStarboardConfig(Snowflake guildId);

    Mono<Starboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId);

    Mono<EmojiDispenser> createEmojiDispenser(Snowflake guildId, Snowflake messageId, Snowflake roleId, EmojiData emojiData);

    Mono<WelcomeMessage> createWelcomeMessage(Snowflake guildId, Snowflake channelId, String message);

    Mono<Poll> createPoll(Snowflake guildId, Snowflake messageId, List<String> options);

    Mono<CommandConfig> createCommandConfig(Snowflake guildId, List<String> names, List<String> aliases);
}
