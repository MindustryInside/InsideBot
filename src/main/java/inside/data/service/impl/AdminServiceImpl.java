package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.audit.*;
import inside.data.entity.*;
import inside.data.repository.AdminActionRepository;
import inside.data.service.*;
import inside.service.DiscordService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.util.annotation.Nullable;

import java.util.Set;

import static inside.audit.Attribute.*;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

@Service
public class AdminServiceImpl implements AdminService{

    private final AdminActionRepository repository;

    private final EntityRetriever entityRetriever;

    private final DiscordService discordService;

    private final AuditService auditService;

    public AdminServiceImpl(@Autowired AdminActionRepository repository,
                            @Autowired EntityRetriever entityRetriever,
                            @Autowired DiscordService discordService,
                            @Autowired AuditService auditService){
        this.repository = repository;
        this.entityRetriever = entityRetriever;
        this.discordService = discordService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId){
        return Flux.defer(() -> Flux.fromIterable(repository.find(type, guildId.asLong(), targetId.asLong())));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> getAll(AdminActionType type){
        return Flux.defer(() -> Flux.fromIterable(repository.findAll(type)));
    }

    @Override
    @Transactional
    public Mono<Void> mute(Member admin, Member target, DateTime end, @Nullable String reason){
        Mono<Void> saveAction = entityRetriever.getLocalMemberById(admin)
                .zipWith(entityRetriever.getLocalMemberById(target))
                .flatMap(function((adminLocalMember, targetLocalMember) -> Mono.fromRunnable(() -> repository.save(AdminAction.builder()
                        .guildId(admin.getGuildId())
                        .type(AdminActionType.mute)
                        .admin(adminLocalMember)
                        .target(targetLocalMember)
                        .reason(reason)
                        .timestamp(DateTime.now())
                        .endTimestamp(end)
                        .build()))));

        Mono<Void> log = auditService.log(admin.getGuildId(), AuditActionType.MEMBER_MUTE)
                .withUser(admin)
                .withTargetUser(target)
                .withAttribute(REASON, reason)
                .withAttribute(DELAY, end.getMillis())
                .save();

        Mono<Void> addRole = entityRetriever.getAdminConfigById(admin.getGuildId())
                .flatMap(adminConfig -> Mono.justOrEmpty(adminConfig.muteRoleID()))
                .flatMap(target::addRole);

        return Mono.when(saveAction, addRole, log);
    }

    @Override
    public Mono<Boolean> isMuted(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.mute, guildId, targetId).hasElements();
    }

    @Override
    @Transactional
    public Mono<Void> unmute(Member target){
        Mono<Void> createIfAbsent = entityRetriever.getLocalMemberById(target)
                .switchIfEmpty(entityRetriever.createLocalMember(target))
                .then();

        Mono<Void> remove = get(AdminActionType.mute, target.getGuildId(), target.getId()).next()
                .flatMap(adminAction -> Mono.fromRunnable(() -> repository.delete(adminAction)))
                .then();

        Mono<Void> log = auditService.log(target.getGuildId(), AuditActionType.MEMBER_UNMUTE)
                .withTargetUser(target)
                .save();

        Mono<Void> removeRole = entityRetriever.getAdminConfigById(target.getGuildId())
                .switchIfEmpty(entityRetriever.createAdminConfig(target.getGuildId()))
                .flatMap(adminConfig -> Mono.justOrEmpty(adminConfig.muteRoleID()))
                .flatMap(target::removeRole);

        return Mono.when(createIfAbsent, removeRole, log, remove);
    }

    @Override
    @Transactional
    public Mono<Void> warn(Member admin, Member target, @Nullable String reason){
        Mono<AdminConfig> getOrCreateAdminConfig = entityRetriever.getAdminConfigById(admin.getGuildId())
                .switchIfEmpty(entityRetriever.createAdminConfig(admin.getGuildId()));

        return Mono.zip(entityRetriever.getLocalMemberById(admin), entityRetriever.getLocalMemberById(target),
                getOrCreateAdminConfig)
                .flatMap(function((adminLocalMember, targetLocalMember, adminConfig) -> Mono.fromRunnable(() -> repository.save(AdminAction.builder()
                        .guildId(admin.getGuildId())
                        .type(AdminActionType.warn)
                        .admin(adminLocalMember)
                        .target(targetLocalMember)
                        .reason(reason)
                        .timestamp(DateTime.now())
                        .endTimestamp(DateTime.now().plus(adminConfig.warnExpireDelay()))
                        .build()))));
    }

    @Override
    @Transactional
    public Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index){
        return warnings(guildId, targetId).elementAt(index)
                .flatMap(action -> Mono.fromRunnable(() -> repository.delete(action)));
    }

    @Override
    public Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.warn, guildId, targetId);
    }

    @Override
    public Mono<Boolean> isOwner(Member member){
        return member.getGuild().map(Guild::getOwnerId).map(ownerId -> member.getId().equals(ownerId));
    }

    @Override
    public Mono<Boolean> isAdmin(Member member){
        Mono<Set<Snowflake>> roles = entityRetriever.getAdminConfigById(member.getGuildId())
                .map(AdminConfig::adminRoleIds);

        Mono<Boolean> isPermissed = member.getRoles().map(Role::getId)
                .filterWhen(id -> roles.map(list -> list.contains(id)))
                .hasElements();

        Mono<Boolean> isAdmin = member.getRoles().map(Role::getPermissions)
                .any(set -> set.contains(Permission.ADMINISTRATOR));

        return Mono.zip(isOwner(member), isAdmin, isPermissed).map(function((owner, admin, permissed) -> owner || admin || permissed));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 */3 * * * *")
    public void warningsMonitor(){
        repository.deleteAllByTypeAndEndTimestampBefore(AdminActionType.warn, DateTime.now());
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    public void mutesMonitor(){
        getAll(AdminActionType.mute)
                .filter(AdminAction::isEnd)
                .flatMap(adminAction -> discordService.gateway()
                        .getMemberById(adminAction.guildId(), adminAction.target().userId()))
                .flatMap(target -> entityRetriever.getGuildConfigById(target.getGuildId()).flatMap(guildConfig ->
                        unmute(target).contextWrite(ctx -> ctx.put(KEY_LOCALE, guildConfig.locale())
                        .put(KEY_TIMEZONE, guildConfig.timeZone()))))
                .subscribe();
    }
}
