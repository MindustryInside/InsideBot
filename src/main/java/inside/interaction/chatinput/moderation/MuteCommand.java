package inside.interaction.chatinput.moderation;

import discord4j.common.util.Snowflake;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.discordjson.possible.Possible;
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
import inside.service.MessageService;
import inside.service.job.UnmuteJob;
import inside.util.MessageUtil;
import io.r2dbc.postgresql.codec.Interval;
import org.reactivestreams.Publisher;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Predicate;

@ChatInputCommand(name = "mute", description = "Выдать мут пользователю. " +
        "Если роль для мута не установлена попытаюсь выдать таймаут",
        permissions = PermissionCategory.MODERATOR)
public class MuteCommand extends ModerationCommand {

    private final ReactiveScheduler reactiveScheduler;

    public MuteCommand(MessageService messageService, EntityRetriever entityRetriever, ReactiveScheduler reactiveScheduler) {
        super(messageService, entityRetriever);
        this.reactiveScheduler = Objects.requireNonNull(reactiveScheduler, "reactiveScheduler");

        addOption(builder -> builder.name("target")
                .description("Нарушитель правил.")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(true));

        addOption(builder -> builder.name("reason")
                .description("Причина мута.")
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        addOption(builder -> builder.name("interval")
                .description("Длительность мута.")
                .type(ApplicationCommandOption.Type.STRING.getValue()));
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

        if (reason != null && reason.length() >= AuditLogEntry.MAX_REASON_LENGTH) {
            return messageService.err(env, "Строка причины слишком длинная (лимит: **%s**)", AuditLogEntry.MAX_REASON_LENGTH);
        }

        String intervalstr = env.getOption("interval")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        return entityRetriever.getModerationConfigById(guildId)
                .zipWith(env.event().getClient().getMemberById(guildId, targetId)
                        .filterWhen(member -> BooleanUtils.not(entityRetriever.getAllModerationActionById(
                                ModerationAction.Type.mute, guildId, member.getId())
                                .hasElements()))
                        .switchIfEmpty(messageService.err(env, "Пользователь уже находится в мьюте").then(Mono.never()))
                        .filter(Predicate.not(User::isBot))
                        .switchIfEmpty(messageService.err(env, "Вы не можете мьютить ботов").then(Mono.never()))
                        .filter(u -> !u.getId().equals(author.getId()))
                        .switchIfEmpty(messageService.err(env, "Вы не можете мьютить самого себя").then(Mono.never()))
                        .filterWhen(u -> u.getBasePermissions()
                                .map(p -> p.equals(PermissionSet.all()) || !p.contains(Permission.ADMINISTRATOR)))
                        .switchIfEmpty(messageService.err(env, "Вы не можете мьютить администраторов").then(Mono.never())))
                .flatMap(TupleUtils.function((config, target) -> {
                    Interval interval = config.muteBaseInterval().orElse(null);
                    if (intervalstr != null) {
                        interval = MessageUtil.parseInterval(intervalstr);

                        if (interval == null) {
                            return messageService.err(env, "Неправильный формат длительности");
                        }
                    }

                    AllowedMentions allowTarget = AllowedMentions.builder().allowUser(targetId).build();

                    Optional<Instant> endTimestamp = Optional.ofNullable(interval)
                            .map(i -> Instant.now().plus(i));

                    ModerationAction action = ModerationAction.builder()
                            .guildId(guildId.asLong())
                            .adminId(author.getId().asLong())
                            .targetId(targetId.asLong())
                            .type(ModerationAction.Type.mute)
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

                    Mono<Void> mute = Mono.justOrEmpty(config.muteRoleId())
                            .map(Snowflake::of)
                            .switchIfEmpty(target.edit()
                                    .withCommunicationDisabledUntil(Possible.of(endTimestamp))
                                    .then(Mono.empty()))
                            .flatMap(l -> target.addRole(l, reason));

                    return messageService.text(env, "Пользователь **%s** получил мьют %s",
                                    MessageUtil.getUserMention(targetId), jcomp)
                            .withAllowedMentions(allowTarget)
                            .and(mute)
                            .then(entityRetriever.save(action))
                            .zipWith(Mono.justOrEmpty(endTimestamp).filter(i -> config.muteRoleId().isPresent()))
                            .flatMap(TupleUtils.function((act, i) -> {
                                JobDetail job = UnmuteJob.createDetails(act);

                                Trigger trigger = SimpleScheduleSpec.builder()
                                        .startTimestamp(i)
                                        .key(UnmuteJob.GROUP, "trigger-" + UUID.randomUUID())
                                        .build()
                                        .asTrigger();

                                return reactiveScheduler.scheduleJob(job, trigger);
                            }));
                }));
    }
}
