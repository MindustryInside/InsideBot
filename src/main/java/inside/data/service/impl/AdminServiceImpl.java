package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.AdminActionRepository;
import inside.data.service.*;
import inside.event.audit.*;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.*;

import java.util.List;

import static inside.event.audit.Attribute.*;
import static inside.util.ContextUtil.*;

@Service
public class AdminServiceImpl implements AdminService{
    private static final Logger log = Loggers.getLogger(AdminService.class);

    private final AdminActionRepository repository;

    private final EntityRetriever entityRetriever;

    private final DiscordService discordService;

    private final Settings settings;

    private final AuditService auditService;

    public AdminServiceImpl(@Autowired AdminActionRepository repository,
                            @Autowired EntityRetriever entityRetriever,
                            @Autowired DiscordService discordService,
                            @Autowired Settings settings,
                            @Autowired AuditService auditService){
        this.repository = repository;
        this.entityRetriever = entityRetriever;
        this.discordService = discordService;
        this.settings = settings;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId){
        return Flux.fromIterable(repository.find(type, guildId.asString(), targetId.asString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdminAction> getAll(AdminActionType type){
        return Flux.fromIterable(repository.findAll(type));
    }

    @Override
    @Transactional
    public Mono<Void> mute(Member admin, Member target, DateTime end, String reason){
        LocalMember adminLocalMember = entityRetriever.getMember(admin);
        LocalMember targetLocalMember = entityRetriever.getMember(target);
        AdminAction action = AdminAction.builder()
                .guildId(admin.getGuildId())
                .type(AdminActionType.mute)
                .admin(adminLocalMember)
                .target(targetLocalMember)
                .reason(reason)
                .timestamp(DateTime.now())
                .endTimestamp(end)
                .build();

        Mono<Void> add = Mono.fromRunnable(() -> repository.save(action));

        Mono<Void> log = auditService.log(admin.getGuildId(), AuditActionType.USER_MUTE)
                .withUser(admin)
                .withTargetUser(target)
                .withAttribute(REASON, reason)
                .withAttribute(DELAY, end.getMillis())
                .save();

        Mono<Void> addRole = Mono.justOrEmpty(entityRetriever.getMuteRoleId(admin.getGuildId()))
                .flatMap(target::addRole);

        return Mono.when(add, addRole, log);
    }

    @Override
    public Mono<Boolean> isMuted(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.mute, guildId, targetId).hasElements();
    }

    @Override
    @Transactional
    public Mono<Void> unmute(Member target){
        LocalMember localMember = entityRetriever.getMember(target);

        Mono<Void> remove = get(AdminActionType.mute, localMember.guildId(), localMember.userId())
                .next()
                .flatMap(adminAction -> Mono.fromRunnable(() -> repository.delete(adminAction)))
                .then();

        Mono<Void> log = auditService.log(localMember.guildId(), AuditActionType.USER_UNMUTE)
                .withTargetUser(target)
                .save();

        Mono<Void> removeRole = Mono.justOrEmpty(entityRetriever.getMuteRoleId(localMember.guildId()))
                .flatMap(target::removeRole);

        return Mono.when(removeRole, log, remove);
    }

    @Override
    @Transactional
    public Mono<Void> warn(Member admin, Member target, String reason){
        LocalMember adminLocalMember = entityRetriever.getMember(admin);
        LocalMember targetLocalMember = entityRetriever.getMember(target);
        AdminConfig config = entityRetriever.getAdminConfigById(admin.getGuildId());
        AdminAction action = AdminAction.builder()
                .guildId(admin.getGuildId())
                .type(AdminActionType.warn)
                .admin(adminLocalMember)
                .target(targetLocalMember)
                .reason(reason)
                .timestamp(DateTime.now())
                .endTimestamp(DateTime.now().plus(config.warnExpireDelay()))
                .build();

        return Mono.fromRunnable(() -> repository.save(action));
    }

    @Override
    @Transactional
    public Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index){
        AdminAction action = repository.find(AdminActionType.warn, guildId.asString(), targetId.asString()).get(index);
        return Mono.justOrEmpty(action).doOnNext(repository::delete).then();
    }

    @Override
    public Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId){
        return get(AdminActionType.warn, guildId, targetId);
    }

    @Override
    public Mono<Boolean> isOwner(Member member){
        if(member == null){
            return Mono.empty();
        }
        return member.getGuild().map(Guild::getOwnerId).map(ownerId -> member.getId().equals(ownerId));
    }

    @Override
    public Mono<Boolean> isAdmin(Member member){
        if(member == null){
            return Mono.empty();
        }
        List<Snowflake> roles = entityRetriever.adminRolesIds(member.getGuildId());

        Mono<Boolean> isPermissed = member.getRoles().map(Role::getId)
                .filter(roles::contains)
                .hasElements();

        Mono<Boolean> isAdmin = member.getRoles().map(Role::getPermissions)
                .any(set -> set.contains(Permission.ADMINISTRATOR));

        return Mono.zip(isOwner(member), isAdmin, isPermissed).map(TupleUtils.function((owner, admin, permissed) -> owner || admin || permissed));
    }

    @Override
    @Scheduled(cron = "0 */3 * * * *")
    public void warningsMonitor(){
        getAll(AdminActionType.warn)
                .filter(AdminAction::isEnd)
                .subscribe(repository::delete);
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    public void mutesMonitor(){
        getAll(AdminService.AdminActionType.mute)
                .filter(AdminAction::isEnd)
                .flatMap(adminAction -> discordService.gateway()
                        .getMemberById(adminAction.guildId(), adminAction.target().userId()))
                .flatMap(target -> unmute(target).contextWrite(ctx -> ctx.put(KEY_LOCALE, entityRetriever.getLocale(target.getGuildId()))
                        .put(KEY_TIMEZONE, entityRetriever.getTimeZone(target.getGuildId()))))
                .subscribe();
    }
}
