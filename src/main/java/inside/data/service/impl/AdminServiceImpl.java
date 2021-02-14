package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.AdminActionRepository;
import inside.data.service.*;
import inside.event.dispatcher.EventType;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;

import java.util.*;

@Service
public class AdminServiceImpl implements AdminService{

    private final AdminActionRepository repository;

    private final EntityRetriever entityRetriever;

    private final DiscordService discordService;

    private final Settings settings;

    public AdminServiceImpl(@Autowired AdminActionRepository repository,
                            @Autowired EntityRetriever entityRetriever,
                            @Autowired DiscordService discordService,
                            @Autowired Settings settings){
        this.repository = repository;
        this.entityRetriever = entityRetriever;
        this.discordService = discordService;
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId){
        return Flux.fromIterable(repository.findAdminActionsByTypeAndTargetId(type, guildId, targetId));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> getAll(AdminActionType type){
        return Flux.fromIterable(repository.findAllByType(type));
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
                .reason(reason);

        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> unban(Snowflake guildId, Snowflake targetId){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.ban, guildId, targetId).get(0);
        return Mono.justOrEmpty(action).doOnNext(repository::delete).then();
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

        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    public Mono<Boolean> isMuted(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.mute, guildId, targetId).hasElements();
    }

    @Override
    @Transactional
    public Mono<Void> unmute(Snowflake guildId, Snowflake targetId){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.mute, guildId, targetId).get(0);
        return Mono.justOrEmpty(action).doOnNext(repository::delete).then();
    }

    @Override
    @Transactional
    public Mono<Void> warn(LocalMember admin, LocalMember target, String reason){
        AdminAction action = new AdminAction(target.guildId())
                .type(AdminActionType.warn)
                .admin(admin)
                .target(target)
                .timestamp(Calendar.getInstance())
                .end(DateTime.now().plusDays(settings.warnExpireDays).toCalendar(entityRetriever.locale(admin.guildId())))
                .reason(reason);

        return Mono.just(action).doOnNext(repository::save).then();
    }

    @Override
    @Transactional
    public Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index){
        AdminAction action = repository.findAdminActionsByTypeAndTargetId(AdminActionType.warn, guildId, targetId).get(index);
        return Mono.justOrEmpty(action).doOnNext(repository::delete).then();
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.warn, guildId, targetId);
    }

    @Override
    public Mono<Boolean> isOwner(Member member){
        if(member == null) return Mono.empty();
        return member.getGuild().map(Guild::getOwnerId).map(ownerId -> member.getId().equals(ownerId));
    }

    @Override
    public Mono<Boolean> isAdmin(Member member){
        if(member == null) return Mono.empty();
        Flux<Snowflake> roles = entityRetriever.adminRolesIds(member.getGuildId());

        Mono<Boolean> isPermissed = member.getRoles().map(Role::getId)
                .filterWhen(roleId -> roles.any(id -> id.equals(roleId)))
                .hasElements();

        Mono<Boolean> isAdmin = member.getRoles().map(Role::getPermissions).any(set -> set.contains(Permission.ADMINISTRATOR));

        return Mono.zip(isOwner(member), isAdmin, isPermissed).map(TupleUtils.function((owner, admin, permissed) -> owner || admin || permissed));
    }

    @Override
    @Scheduled(cron = "0 */3 * * * *")
    public void warningsMonitor(){
        Flux.fromIterable(repository.findAllByType(AdminActionType.warn))
                .filter(AdminAction::isEnd)
                .subscribe(repository::delete);
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    public void mutesMonitor(){
        getAll(AdminService.AdminActionType.mute)
                .filter(AdminAction::isEnd)
                .subscribe(adminAction -> discordService.eventListener().publish(
                        new EventType.MemberUnmuteEvent(discordService.gateway().getGuildById(adminAction.guildId()).block(), adminAction.target())
                ));
    }
}
