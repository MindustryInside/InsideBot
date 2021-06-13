package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import inside.data.service.actions.*;
import inside.data.service.api.Store;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

@Service
public class EntityRetrieverImpl implements EntityRetriever{

    private final Store store;

    private final Settings settings;

    private final MessageService messageService;

    public EntityRetrieverImpl(@Autowired Store store,
                               @Autowired Settings settings,
                               @Autowired MessageService messageService){
        this.store = store;
        this.settings = settings;
        this.messageService = messageService;
    }

    @Override
    public Mono<GuildConfig> getGuildConfigById(Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getGuildConfigById(guildId.asLong())));
    }

    @Override
    public Mono<Void> save(GuildConfig guildConfig){
        return Mono.from(store.execute(UpdateStoreActions.guildConfigSave(guildConfig)));
    }

    @Override
    public Mono<AdminConfig> getAdminConfigById(Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getAdminConfigById(guildId.asLong())));
    }

    @Override
    public Mono<Void> save(AdminConfig adminConfig){
        return Mono.from(store.execute(UpdateStoreActions.adminConfigSave(adminConfig)));
    }

    @Override
    public Mono<AuditConfig> getAuditConfigById(Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getAuditConfigById(guildId.asLong())));
    }

    @Override
    public Mono<Void> save(AuditConfig auditConfig){
        return Mono.from(store.execute(UpdateStoreActions.auditConfigSave(auditConfig)));
    }

    @Override
    public Flux<LocalMember> getAllLocalMembers(){
        return Flux.from(store.execute(ReadStoreActions.getAllLocalMembers()));
    }

    @Override
    public Mono<LocalMember> getLocalMemberById(Snowflake userId, Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getLocalMemberById(userId.asLong(), guildId.asLong())));
    }

    @Override
    public Mono<LocalMember> getAndUpdateLocalMemberById(Member member){
        Snowflake userId = member.getId();
        Snowflake guildId = member.getGuildId();
        return getLocalMemberById(userId, guildId)
                .map(localMember -> {
                    localMember.effectiveName(member.getDisplayName());
                    return localMember;
                });
    }

    @Override
    public Mono<Void> save(LocalMember localMember){
        return Mono.from(store.execute(UpdateStoreActions.localMemberSave(localMember)));
    }

    @Override
    public Mono<MessageInfo> getMessageInfoById(Snowflake messageId){
        return Mono.from(store.execute(ReadStoreActions.getMessageInfoById(messageId.asLong())));
    }

    @Override
    public Mono<Void> deleteMessageInfoById(Snowflake messageId){
        return getMessageInfoById(messageId).flatMap(this::delete);
    }

    @Override
    public Mono<Void> delete(MessageInfo messageInfo){
        return Mono.from(store.execute(UpdateStoreActions.messageInfoDelete(messageInfo)));
    }

    @Override
    public Mono<Void> save(MessageInfo messageInfo){
        return Mono.from(store.execute(UpdateStoreActions.messageInfoSave(messageInfo)));
    }

    @Override
    public Mono<StarboardConfig> getStarboardConfigById(Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getStarboardConfigById(guildId.asLong())));
    }

    @Override
    public Mono<Void> save(StarboardConfig starboardConfig){
        return Mono.from(store.execute(UpdateStoreActions.starboardConfigSave(starboardConfig)));
    }

    @Override
    public Mono<Starboard> getStarboardById(Snowflake guildId, Snowflake sourceMessageId){
        return Mono.from(store.execute(ReadStoreActions.getStarboardById(guildId.asLong(), sourceMessageId.asLong())));
    }

    @Override
    public Mono<Void> deleteStarboardById(Snowflake guildId, Snowflake sourceMessageId){
        return getStarboardById(guildId, sourceMessageId).flatMap(this::delete);
    }

    @Override
    public Mono<Void> delete(Starboard starboard){
        return Mono.from(store.execute(UpdateStoreActions.starboardDelete(starboard)));
    }

    @Override
    public Mono<Void> save(Starboard starboard){
        return Mono.from(store.execute(UpdateStoreActions.starboardSave(starboard)));
    }

    @Override
    public Mono<ActiveUserConfig> getActiveUserConfigById(Snowflake guildId){
        return Mono.from(store.execute(ReadStoreActions.getActiveUserConfigById(guildId.asLong())));
    }

    @Override
    public Mono<Void> save(ActiveUserConfig activeUserConfig){
        return Mono.from(store.execute(UpdateStoreActions.activeUserConfigSave(activeUserConfig)));
    }

    @Override
    public Mono<GuildConfig> createGuildConfig(Snowflake guildId){
        return Mono.defer(() -> {
            GuildConfig guildConfig = new GuildConfig();
            guildConfig.guildId(guildId);
            guildConfig.prefix(settings.getDefaults().getPrefix());
            guildConfig.locale(LocaleUtil.getDefaultLocale());
            guildConfig.timeZone(settings.getDefaults().getTimeZone());
            return save(guildConfig).thenReturn(guildConfig);
        });
    }

    @Override
    public Mono<AdminConfig> createAdminConfig(Snowflake guildId){
        return Mono.defer(() -> {
            AdminConfig adminConfig = new AdminConfig();
            adminConfig.guildId(guildId);
            adminConfig.maxWarnCount(settings.getDefaults().getMaxWarnings());
            adminConfig.muteBaseDelay(Duration.millis(settings.getDefaults().getMuteEvade().toMillis())); // TODO: use joda or java.time?
            adminConfig.warnExpireDelay(Duration.millis(settings.getDefaults().getWarnExpire().toMillis()));
            return save(adminConfig).thenReturn(adminConfig);
        });
    }

    @Override
    public Mono<AuditConfig> createAuditConfig(Snowflake guildId){
        return Mono.defer(() -> {
            AuditConfig auditConfig = new AuditConfig();
            auditConfig.guildId(guildId);
            return save(auditConfig).thenReturn(auditConfig);
        });
    }

    @Override
    public Mono<LocalMember> createLocalMember(Snowflake userId, Snowflake guildId, String effectiveNickname){
        return Mono.defer(() -> {
            LocalMember localMember = new LocalMember();
            localMember.userId(userId);
            localMember.guildId(guildId);
            localMember.effectiveName(effectiveNickname);
            Activity activity = new Activity();
            activity.guildId(guildId);
            localMember.activity(activity); // TODO: lazy initializing?
            return save(localMember).thenReturn(localMember);
        });
    }

    @Override
    public Mono<MessageInfo> createMessageInfo(Message message){
        return Mono.defer(() -> {
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.messageId(message.getId());
            messageInfo.userId(message.getAuthor().map(User::getId).orElseThrow(IllegalStateException::new)); // only users, not webhooks
            messageInfo.guildId(message.getGuildId().orElseThrow(IllegalStateException::new)); // only guilds
            messageInfo.timestamp(new DateTime(message.getTimestamp().toEpochMilli()));
            messageInfo.content(messageService.encrypt(MessageUtil.effectiveContent(message), message.getId(), message.getChannelId()));
            return save(messageInfo).thenReturn(messageInfo);
        });
    }

    @Override
    public Mono<StarboardConfig> createStarboardConfig(Snowflake guildId){
        return Mono.defer(() -> {
            StarboardConfig starboardConfig = new StarboardConfig();
            starboardConfig.guildId(guildId);
            starboardConfig.lowerStarBarrier(settings.getDefaults().getStarboardLowerStarBarrier());
            return save(starboardConfig).thenReturn(starboardConfig);
        });
    }

    @Override
    public Mono<ActiveUserConfig> createActiveUserConfig(Snowflake guildId){
        return Mono.defer(() -> {
            ActiveUserConfig activeUserConfig = new ActiveUserConfig();
            activeUserConfig.guildId(guildId);
            activeUserConfig.keepCountingPeriod(settings.getDefaults().getActiveUserKeepCountingPeriod());
            activeUserConfig.messageBarrier(settings.getDefaults().getActiveUserMessageBarrier());
            return save(activeUserConfig).thenReturn(activeUserConfig);
        });
    }

    @Override
    public Mono<Starboard> createStarboard(Snowflake guildId, Snowflake sourceMessageId, Snowflake targetMessageId){
        return Mono.defer(() -> {
            Starboard starboard = new Starboard();
            starboard.guildId(guildId);
            starboard.sourceMessageId(sourceMessageId);
            starboard.targetMessageId(targetMessageId);
            return save(starboard).thenReturn(starboard);
        });
    }
}
