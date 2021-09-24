package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.*;
import discord4j.rest.util.Permission;
import inside.audit.AuditService;
import inside.data.entity.AdminConfig;
import inside.data.service.EntityRetriever;
import inside.data.service.impl.WelcomeMessageService;
import inside.resolver.MessageTemplate;
import inside.service.*;
import inside.util.DiscordUtil;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.*;
import java.util.*;
import java.util.function.Predicate;

import static inside.audit.Attribute.*;
import static inside.audit.AuditActionType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends ReactiveEventAdapter{
    // the number from which the filtering of audit logs is based
    protected static final long TIMEOUT_MILLIS = 3500L;

    @Autowired
    private EntityRetriever entityRetriever;

    @Lazy
    @Autowired
    private AuditService auditService;

    @Lazy
    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WelcomeMessageService welcomeMessageService;

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();

        Snowflake guildId = member.getGuildId();

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<AdminConfig> adminConfig = entityRetriever.getAdminConfigById(member.getGuildId());

        Mono<Void> warn = Mono.deferContextual(ctx -> member.getGuild().flatMap(Guild::getOwner)
                .filterWhen(ignored -> adminConfig.flatMap(config -> adminService.warnings(member).count()
                        .map(c -> c >= config.getMaxWarnCount())))
                .flatMap(owner -> adminService.warn(owner, member, messageService.get(ctx, "audit.member.warn.evade"))));

        Mono<Void> muteEvade = Mono.deferContextual(ctx -> member.getGuild().flatMap(Guild::getOwner)
                        .filterWhen(ignored -> adminService.isMuted(member))
                        .flatMap(owner -> adminConfig.flatMap(config -> adminService.mute(owner, member,
                                        Instant.now().plus(config.getMuteBaseDelay()),
                                        messageService.get(ctx, "audit.member.mute.evade"))
                                .thenReturn(owner)))
                        .switchIfEmpty(warn.then(Mono.empty())))
                .then();

        Mono<Void> log = auditService.newBuilder(event.getGuildId(), MEMBER_JOIN)
                .withUser(member)
                .save();

        //because incorrectly defines a reference to a method
        //noinspection Convert2MethodRef
        Mono<Void> welcomeMessage = entityRetriever.getWelcomeMessageById(guildId)
                .flatMap(welcomeMessage1 -> event.getClient().getChannelById(welcomeMessage1.getChannelId())
                        .cast(TopLevelGuildMessageChannel.class)
                        .zipWith(welcomeMessageService.compile(MessageTemplate.builder()
                                .member(member)
                                .template(welcomeMessage1)
                                .build()))
                        .flatMap(TupleUtils.function((channel, spec) -> channel.createMessage(spec))))
                .then();

        Mono<Void> returnRoles = Mono.deferContextual(ctx -> entityRetriever.getLocalMemberById(member.getId(), member.getGuildId())
                        .zipWith(event.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                .getSelfMember(member.getGuildId()))
                        .filterWhen(TupleUtils.function((localMember, self) -> self.getBasePermissions()
                                .map(set -> set.contains(Permission.MANAGE_ROLES))))
                        .filter(TupleUtils.predicate((localMember, self) -> !member.isBot()))
                        .flatMapMany(TupleUtils.function((localMember, self) -> self.getHighestRole().zipWith(self.getGuild())
                                .flatMapMany(TupleUtils.function((highest, guild) -> Flux.fromIterable(localMember.getLastRoleIds())
                                        .flatMap(guild::getRoleById)
                                        .filter(role -> highest.getRawPosition() > role.getRawPosition())
                                        .map(Role::getId)))))
                        .collectList()
                        .flatMap(roleIds -> member.edit(GuildMemberEditSpec.builder()
                                .roles(roleIds)
                                .reason(messageService.get(ctx, "common.auto-roles"))
                                .build())))
                .then();

        return initContext.flatMap(context -> Mono.when(log, muteEvade, welcomeMessage, returnRoles).contextWrite(context));
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();

        Snowflake guildId = event.getGuildId();

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<Void> log = auditService.newBuilder(event.getGuildId(), MEMBER_LEAVE)
                .withUser(user)
                .save();

        Mono<Void> kick = Mono.deferContextual(ctx -> event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(AuditLogQuerySpec.builder()
                        .actionType(ActionType.MEMBER_KICK)
                        .build()))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getId().getTimestamp()
                        .isAfter(Instant.now(Clock.systemUTC())
                                .minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(targetId -> targetId.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> Mono.justOrEmpty(entry.getResponsibleUser())
                        .flatMap(admin -> auditService.newBuilder(guildId, MEMBER_KICK)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(ctx, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(log.then(Mono.empty()))
                .then());

        return initContext.flatMap(ctx -> event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(AuditLogQuerySpec.builder()
                        .actionType(ActionType.MEMBER_BAN_ADD)
                        .build()))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getId().getTimestamp()
                        .isAfter(Instant.now(Clock.systemUTC())
                                .minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(targetId -> targetId.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> Mono.justOrEmpty(entry.getResponsibleUser())
                        .flatMap(admin -> auditService.newBuilder(guildId, MEMBER_BAN)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(ctx, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(kick.then(Mono.empty()))
                .contextWrite(ctx));
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        Member old = event.getOld().orElse(null);
        Snowflake guildId = event.getGuildId();
        if(old == null){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        return initContext.flatMap(context -> event.getMember()
                .filter(Predicate.not(DiscordUtil::isBot))
                .flatMap(member -> entityRetriever.getLocalMemberById(member.getId(), guildId)
                        .switchIfEmpty(entityRetriever.createLocalMember(member))
                        .flatMap(localMember -> {
                            Mono<Void> logAvatarUpdate = Mono.defer(() -> {
                                if(!old.getAvatarUrl().equals(member.getAvatarUrl())){
                                    return auditService.newBuilder(guildId, MEMBER_AVATAR_UPDATE)
                                            .withUser(member)
                                            .withAttribute(AVATAR_URL, member.getAvatarUrl())
                                            .withAttribute(OLD_AVATAR_URL, old.getAvatarUrl())
                                            .save();
                                }
                                return Mono.empty();
                            });

                            Mono<Void> logRoleUpdate = Mono.defer(() -> {
                                Set<Snowflake> oldRoleIds = old.getRoleIds();
                                if(event.getCurrentRoleIds().equals(oldRoleIds)){
                                    return logAvatarUpdate;
                                }

                                localMember.setLastRoleIds(event.getCurrentRoleIds());
                                boolean added = oldRoleIds.size() < event.getCurrentRoleIds().size();
                                List<Snowflake> difference = new ArrayList<>(added
                                        ? event.getCurrentRoleIds()
                                        : oldRoleIds);
                                difference.removeIf((added ? oldRoleIds : event.getCurrentRoleIds())::contains);

                                return auditService.newBuilder(guildId, added ? MEMBER_ROLE_ADD : MEMBER_ROLE_REMOVE)
                                        .withUser(member)
                                        .withAttribute(AVATAR_URL, member.getAvatarUrl())
                                        .withAttribute(ROLE_IDS, difference)
                                        .save();
                            });

                            String effectiveName = event.getCurrentNickname().orElseGet(member::getUsername);

                            Mono<Void> logNicknameUpdate = Mono.defer(() -> {
                                if(effectiveName.equals(old.getDisplayName())){
                                    return Mono.empty();
                                }

                                return auditService.newBuilder(guildId, MEMBER_NICKNAME_UPDATE)
                                        .withUser(member)
                                        .withAttribute(AVATAR_URL, member.getAvatarUrl())
                                        .withAttribute(OLD_NICKNAME, old.getDisplayName())
                                        .withAttribute(NEW_NICKNAME, effectiveName)
                                        .save();
                            });

                            localMember.setEffectiveName(effectiveName);
                            return Mono.when(logRoleUpdate, logNicknameUpdate)
                                    .then(entityRetriever.save(localMember));
                        })).contextWrite(context));
    }

    @Override
    public Publisher<?> onGuildDelete(GuildDeleteEvent event){ // remove all content associated with this guild id
        if(event.isUnavailable()){ // guild currently disabled
            return Mono.empty();
        }
        Snowflake guildId = event.getGuildId();
        return entityRetriever.deleteAllStarboardsInGuild(guildId)
                .and(entityRetriever.deleteAllEmojiDispenserInGuild(guildId))
                .and(entityRetriever.deleteAllLocalMembersInGuild(guildId))
                .and(entityRetriever.deleteAllMessageInfoInGuild(guildId))
                .and(entityRetriever.deleteAuditConfigById(guildId))
                .and(entityRetriever.deleteGuildConfigById(guildId))
                .and(entityRetriever.deleteAdminConfigById(guildId))
                .and(entityRetriever.deleteActivityConfigById(guildId))
                .and(entityRetriever.deleteStarboardConfigById(guildId))
                .and(entityRetriever.deleteAllPollInGuild(guildId));
    }
}
