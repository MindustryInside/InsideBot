package inside.data.service.api;

import discord4j.store.api.util.LongLongTuple2;
import inside.data.entity.*;
import inside.data.service.StoreHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

@Service
public class DefaultEntityStoreLayout implements EntityStoreLayout, EntityAccessor, EntityUpdater{

    @Autowired
    private StoreHolder storeHolder;

    @Override
    public EntityAccessor getEntityAccessor(){
        return this;
    }

    @Override
    public EntityUpdater getEntityUpdater(){
        return this;
    }

    @Override
    public Mono<GuildConfig> getGuildConfigById(long guildId){
        return storeHolder.getGuildConfigService()
                .find(guildId);
    }

    @Override
    public Mono<AdminConfig> getAdminConfigById(long guildId){
        return storeHolder.getAdminConfigService()
                .find(guildId);
    }

    @Override
    public Mono<AuditConfig> getAuditConfigById(long guildId){
        return storeHolder.getAuditConfigService()
                .find(guildId);
    }

    @Override
    public Flux<LocalMember> getAllLocalMembers(){
        return storeHolder.getLocalMemberService()
                .getAll();
    }

    @Override
    public Mono<LocalMember> getLocalMemberById(long userId, long guildId){
        return storeHolder.getLocalMemberService()
                .find(LongLongTuple2.of(userId, guildId));
    }

    @Override
    public Mono<MessageInfo> getMessageInfoById(long messageId){
        return storeHolder.getMessageInfoService()
                .find(messageId);
    }

    @Override
    public Mono<StarboardConfig> getStarboardConfigById(long guildId){
        return storeHolder.getStarboardConfigService()
                .find(guildId);
    }

    @Override
    public Mono<Starboard> getStarboardById(long guildId, long sourceMessageId){
        return storeHolder.getStarboardService()
                .find(LongLongTuple2.of(guildId, sourceMessageId));
    }

    @Override
    public Mono<Void> onGuildConfigSave(GuildConfig guildConfig){
        return storeHolder.getGuildConfigService()
                .save(guildConfig);
    }

    @Override
    public Mono<Void> onAdminConfigSave(AdminConfig adminConfig){
        return storeHolder.getAdminConfigService()
                .save(adminConfig);
    }

    @Override
    public Mono<Void> onAuditConfigSave(AuditConfig auditConfig){
        return storeHolder.getAuditConfigService()
                .save(auditConfig);
    }

    @Override
    public Mono<Void> onLocalMemberSave(LocalMember localMember){
        return storeHolder.getLocalMemberService()
                .save(localMember);
    }

    @Override
    public Mono<Void> onMessageInfoSave(MessageInfo messageInfo){
        return storeHolder.getMessageInfoService()
                .save(messageInfo);
    }

    @Override
    public Mono<Void> onMessageInfoDelete(MessageInfo messageInfo){
        return storeHolder.getMessageInfoService()
                .delete(messageInfo);
    }

    @Override
    public Mono<Void> onStarboardConfigSave(StarboardConfig starboardConfig){
        return storeHolder.getStarboardConfigService()
                .save(starboardConfig);
    }

    @Override
    public Mono<Void> onStarboardSave(Starboard starboard){
        return storeHolder.getStarboardService()
                .save(starboard);
    }

    @Override
    public Mono<Void> onStarboardDelete(Starboard starboard){
        return storeHolder.getStarboardService()
                .delete(starboard);
    }
}
