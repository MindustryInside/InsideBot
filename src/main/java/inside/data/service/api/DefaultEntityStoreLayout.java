package inside.data.service.api;

import discord4j.store.api.util.LongLongTuple2;
import inside.data.entity.*;
import inside.data.service.StoreHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DefaultEntityStoreLayout implements EntityStoreLayout{

    @Autowired
    private StoreHolder storeHolder;

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
}
