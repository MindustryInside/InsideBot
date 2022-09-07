package inside.interaction.chatinput.moderation;

import discord4j.common.util.Snowflake;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.data.schedule.JobDetail;
import inside.data.schedule.ReactiveScheduler;
import inside.data.schedule.SimpleScheduleSpec;
import inside.data.schedule.Trigger;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Option;
import inside.service.MessageService;
import inside.service.job.UnmuteJob;
import inside.util.MessageUtil;
import io.r2dbc.postgresql.codec.Interval;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Predicate;

import static inside.data.entity.ModerationAction.timeoutLimit;

@ChatInputCommand(value = "commands.moderation.warn", permissions = PermissionCategory.MODERATOR)
@Option(name = "target", type = Type.USER, required = true)
@Option(name = "reason", type = Type.STRING, maxLength = AuditLogEntry.MAX_REASON_LENGTH)
@Option(name = "interval", type = Type.STRING)
@Option(name = "count", type = Type.INTEGER, minValue = 1, maxValue = Integer.MAX_VALUE)
public class WarnCommand extends ModerationCommand {

    private final ReactiveScheduler reactiveScheduler;

    public WarnCommand(MessageService messageService, EntityRetriever entityRetriever,
                       ReactiveScheduler reactiveScheduler) {
        super(messageService, entityRetriever);
        this.reactiveScheduler = reactiveScheduler;
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {

        Member author = env.event().getInteraction().getMember().orElseThrow();
        Snowflake guildId = author.getGuildId();

        Snowflake targetId = env.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                .orElseThrow();

        String reason = env.getOption("reason")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        String intervalstr = env.getOption("time")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        int count = env.getOption("count")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Math::toIntExact)
                .orElse(1);

        return entityRetriever.getModerationConfigById(guildId)
                .zipWith(env.event().getClient().getMemberById(guildId, targetId)
                        .filter(Predicate.not(User::isBot))
                        .switchIfEmpty(messageService.err(env, "commands.moderation.warn.target-is-bot").then(Mono.never()))
                        .filter(u -> !u.getId().equals(author.getId()))
                        .switchIfEmpty(messageService.err(env, "commands.moderation.warn.self-warn").then(Mono.never()))
                        .filterWhen(u -> u.getBasePermissions()
                                .map(p -> p.equals(PermissionSet.all()) || !p.contains(Permission.ADMINISTRATOR)))
                        .switchIfEmpty(messageService.err(env, "commands.moderation.warn.target-is-admin").then(Mono.never())))
                .flatMap(TupleUtils.function((config, target) -> {
                    Interval interval = config.warnExpireInterval().orElse(null);
                    if (intervalstr != null) {
                        interval = MessageUtil.parseInterval(intervalstr);

                        if (interval == null) {
                            return messageService.err(env, "common.invalid-interval-format");
                        }
                    }

                    AllowedMentions allowTarget = AllowedMentions.builder().allowUser(targetId).build();

                    var endTimestamp = Optional.ofNullable(interval)
                            .map(i -> Instant.now().plus(i));

                    ModerationAction action = ModerationAction.builder()
                            .guildId(guildId.asLong())
                            .adminId(author.getId().asLong())
                            .targetId(targetId.asLong())
                            .type(ModerationAction.Type.warn)
                            .reason(Optional.ofNullable(reason))
                            .endTimestamp(endTimestamp)
                            .build();

                    String byReason = reason != null ? String.format("по причине *%s*", reason) : null;
                    String until = endTimestamp.map(i -> String.format("до *%s*", TimestampFormat.LONG_DATE_TIME.format(i)))
                            .orElse(null);
                    StringJoiner jcomp = new StringJoiner(" ");
                    if (byReason != null) {
                        jcomp.add(byReason);
                    }
                    if (until != null) {
                        jcomp.add(until);
                    }

                    return Mono.defer(() -> {
                        if (count == 1) {
                            return messageService.text(env, "Пользователь **%s** получил предупреждение %s",
                                            MessageUtil.getUserMention(targetId), jcomp)
                                    .withAllowedMentions(allowTarget)
                                    .and(entityRetriever.save(action));
                        }

                        return messageService.text(env, "Пользователь **%s** получил **%s** %s %s", MessageUtil.getUserMention(targetId),
                                        count, messageService.getPluralized(env.context(), "common.warn", count), jcomp)
                                .withAllowedMentions(allowTarget)
                                .and(entityRetriever.save(action)
                                        .repeat(count - 1));
                    })
                    .then(entityRetriever.moderationActionCountById(ModerationAction.Type.warn, guildId, targetId))
                    .flatMap(l -> Mono.justOrEmpty(config.thresholdPunishments()).mapNotNull(t -> t.get(l)))
                    .flatMap(sett -> switch (sett.type()) {
                        case warn -> {
                            Optional<Instant> inst = Possible.flatOpt(sett.interval())
                                    .or(config::warnExpireInterval)
                                    .map(i -> Instant.now().plus(i));

                            ModerationAction autoWarn = ModerationAction.builder()
                                    .guildId(guildId.asLong())
                                    .adminId(env.event().getClient().getSelfId().asLong())
                                    .targetId(targetId.asLong())
                                    .type(ModerationAction.Type.warn)
                                    .endTimestamp(inst)
                                    .reason("Авто-пред")
                                    .build();

                            String autoWarnUntil = inst.map(i -> String.format("до *%s*", TimestampFormat.LONG_DATE_TIME.format(i)))
                                    .orElse(null);
                            StringJoiner ajcomp = new StringJoiner(" ");
                            if (byReason != null) {
                                ajcomp.add(byReason);
                            }
                            if (autoWarnUntil != null) {
                                ajcomp.add(autoWarnUntil);
                            }

                            yield target.getPrivateChannel()
                                    .onErrorResume(e -> e instanceof ClientException, e -> Mono.empty())
                                    .flatMap(c -> c.createMessage(String.format("Вы получили автоматическое предупреждение за нарушение правил %s", ajcomp)))
                                    .and(entityRetriever.save(autoWarn));
                        }
                        case mute -> {
                            Instant now = Instant.now();
                            Optional<Interval> inter = Possible.flatOpt(sett.interval())
                                    .or(config::muteBaseInterval);
                            Optional<Instant> inst = inter.map(now::plus);

                            Optional<Snowflake> muteRoleId = config.muteRoleId()
                                    .map(Snowflake::of);

                            ModerationAction autoMute = ModerationAction.builder()
                                    .guildId(guildId.asLong())
                                    .adminId(env.event().getClient().getSelfId().asLong())
                                    .targetId(targetId.asLong())
                                    .type(ModerationAction.Type.mute)
                                    .endTimestamp(inst)
                                    .reason("Авто-мут")
                                    .build();

                            String autoMuteUntil = inst.map(i -> String.format("до *%s*",
                                            TimestampFormat.LONG_DATE_TIME.format(i)))
                                    .orElse(null);
                            StringJoiner ajcomp = new StringJoiner(" ");
                            if (byReason != null) {
                                ajcomp.add(byReason);
                            }
                            if (autoMuteUntil != null) {
                                ajcomp.add(autoMuteUntil);
                            }

                            yield Mono.justOrEmpty(muteRoleId)
                                    .flatMap(target::addRole)
                                    .switchIfEmpty(target.edit().withReason(reason)
                                            .withCommunicationDisabledUntilOrNull(now.plus(
                                                    inter.filter(i -> i.getSeconds() > timeoutLimit.getSeconds())
                                                            .orElse(timeoutLimit)))
                                            .then(Mono.never()))
                                    .onErrorResume(ClientException.class, e -> Mono.empty())
                                    .and(target.getPrivateChannel()
                                            .onErrorResume(ClientException.class, e -> Mono.empty())
                                            .flatMap(c -> c.createMessage("Вы получили автоматический мут за нарушение правил " + ajcomp)))
                                    .then(entityRetriever.save(autoMute))
                                    .zipWith(Mono.justOrEmpty(inst).filter(i -> muteRoleId.isPresent()))
                                    .flatMap(TupleUtils.function((act, i) -> {
                                        JobDetail job = UnmuteJob.createDetails(act);

                                        Trigger trigger = SimpleScheduleSpec.builder()
                                                .startTimestamp(i)
                                                .key(UnmuteJob.GROUP, "trigger-" + UUID.randomUUID())
                                                .build()
                                                .asTrigger();

                                        return reactiveScheduler.scheduleJob(job, trigger);
                                    }));
                        }
                    });
                }));
    }
}
