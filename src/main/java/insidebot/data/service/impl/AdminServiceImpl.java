package insidebot.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import insidebot.data.entity.*;
import insidebot.data.repository.AdminActionRepository;
import insidebot.data.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;

import java.util.Calendar;

@Service
public class AdminServiceImpl implements AdminService{
    @Autowired
    private AdminActionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId){
        return Flux.fromIterable(repository.findAdminActionsByTypeAndTargetId(type, guildId, targetId));
    }

    @Override
    @Transactional
    public Mono<Void> kick(LocalMember admin, LocalMember target, String reason){
        AdminAction action = new AdminAction(target.guildId())
                .type(AdminActionType.kick)
                .admin(admin)
                .target(target)
                .timestamp(Calendar.getInstance())
                .reason(reason);
        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> ban(LocalMember admin, LocalMember target, String reason){
        AdminAction action = new AdminAction(target.guildId())
                .type(AdminActionType.ban)
                .admin(admin)
                .target(target)
                .timestamp(Calendar.getInstance())
                //может сделать авторазбан?
                .reason(reason);
        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> unban(Snowflake guildId, Snowflake targetId){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.ban, guildId, targetId).get(0);
        return Mono.just(action).doOnNext(repository::delete).then();
    }

    @Override
    @Transactional
    public Mono<Void> mute(LocalMember admin, LocalMember target, Calendar end, String reason){
        AdminAction action = new AdminAction(target.guildId())
                .type(AdminActionType.mute)
                .admin(admin)
                .target(target)
                .timestamp(Calendar.getInstance())
                .end(end)
                .reason(reason);
        //todo
        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> unmute(Snowflake guildId, Snowflake targetId){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.mute, guildId, targetId).get(0);
        return Mono.just(action).doOnNext(repository::delete).then();
    }

    @Override
    @Transactional
    public Mono<Void> warn(LocalMember admin, LocalMember target, String reason){
        AdminAction action = new AdminAction(target.guildId())
                .type(AdminActionType.warn)
                .admin(admin)
                .target(target)
                .timestamp(Calendar.getInstance())
                .reason(reason);
        //todo логгирование
        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.warn, guildId, targetId).get(index);
        return Mono.fromRunnable(() -> repository.delete(action));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId){
        return Flux.fromIterable(repository.findAdminActionsByTypeAndTargetId(AdminActionType.warn, guildId, targetId));
    }

    @Override
    public boolean isAdmin(Member member){
        return member != null && (isOwner(member) || member.getRoles().map(Role::getPermissions)
                                                           .any(r -> r.contains(Permission.ADMINISTRATOR))
                                                           .blockOptional().orElse(false));
    }

    @Override
    public boolean isOwner(Member member){
        return member != null && member.getGuild().map(Guild::getOwnerId).map(s -> member.getId().equals(s))
                                       .blockOptional().orElse(false);
    }
}
