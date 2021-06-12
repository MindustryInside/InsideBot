package inside.data.service.api;

import discord4j.common.store.api.ActionMapper;
import inside.data.service.actions.ReadStoreActions.*;
import inside.data.service.actions.UpdateStoreActions.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Service
public class Store{

    private final ActionMapper actionMapper;

    private Store(@Autowired ActionMapper actionMapper){
        this.actionMapper = actionMapper;
    }

    @Bean("actionMapper")
    public static ActionMapper layoutToMapper(EntityStoreLayout layout){
        ActionMapper entityAccessorMapper = entityAccessorToMapper(layout.getEntityAccessor());
        ActionMapper entityUpdaterMapper = entityUpdaterToMapper(layout.getEntityUpdater());
        return ActionMapper.aggregate(entityAccessorMapper, entityUpdaterMapper);
    }

    private static ActionMapper entityAccessorToMapper(EntityAccessor entityAccessor){
        Objects.requireNonNull(entityAccessor, "entityAccessor");
        return ActionMapper.builder()
                .map(GetGuildConfigAction.class, action -> entityAccessor.getGuildConfigById(action.guildId))
                .map(GetAdminConfigAction.class, action -> entityAccessor.getAdminConfigById(action.guildId))
                .map(GetAuditConfigAction.class, action -> entityAccessor.getAuditConfigById(action.guildId))
                .map(GetLocalMemberAction.class, action -> entityAccessor.getLocalMemberById(action.userId, action.guildId))
                .map(GetAllLocalMembersAction.class, action -> entityAccessor.getAllLocalMembers())
                .map(GetMessageInfoAction.class, action -> entityAccessor.getMessageInfoById(action.messageId))
                .map(GetStarboardConfigAction.class, action -> entityAccessor.getStarboardConfigById(action.guildId))
                .map(GetStarboardAction.class, action -> entityAccessor.getStarboardById(action.guildId, action.sourceMessageId))
                .build();
    }

    private static ActionMapper entityUpdaterToMapper(EntityUpdater entityUpdater){
        Objects.requireNonNull(entityUpdater, "entityUpdater");
        return ActionMapper.builder()
                .map(GuildConfigSaveAction.class, action -> entityUpdater.onGuildConfigSave(action.guildConfig))
                .map(AdminConfigSaveAction.class, action -> entityUpdater.onAdminConfigSave(action.adminConfig))
                .map(AuditConfigSaveAction.class, action -> entityUpdater.onAuditConfigSave(action.auditConfig))
                .map(LocalMemberSaveAction.class, action -> entityUpdater.onLocalMemberSave(action.localMember))
                .map(LocalMemberSaveAction.class, action -> entityUpdater.onLocalMemberSave(action.localMember))
                .map(MessageInfoSaveAction.class, action -> entityUpdater.onMessageInfoSave(action.messageInfo))
                .map(MessageInfoDeleteAction.class, action -> entityUpdater.onMessageInfoDelete(action.messageInfo))
                .map(StarboardConfigSaveAction.class, action -> entityUpdater.onStarboardConfigSave(action.starboardConfig))
                .map(StarboardSaveAction.class, action -> entityUpdater.onStarboardSave(action.starboard))
                .map(StarboardDeleteAction.class, action -> entityUpdater.onStarboardDelete(action.starboard))
                .build();
    }

    public <R> Publisher<R> execute(EntityStoreAction<R> action){
        return actionMapper.findHandlerForAction(action)
                .<Publisher<R>>map(h -> h.apply(action))
                .orElse(Flux.empty());
    }
}
