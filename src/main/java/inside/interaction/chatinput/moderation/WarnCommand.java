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
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.data.entity.PunishmentSettings;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.service.MessageService;
import inside.util.MessageUtil;
import io.r2dbc.postgresql.codec.Interval;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;

@ChatInputCommand(name = "warn", description = "Выдать предупреждение пользователю.", permissions = PermissionCategory.MODERATOR)
public class WarnCommand extends ModerationCommand {

    public WarnCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addOption(builder -> builder.name("target")
                .description("Нарушитель правил.")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(true));

        addOption(builder -> builder.name("reason")
                .description("Причина предупреждения.")
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        addOption(builder -> builder.name("interval")
                .description("Длительность предупреждений.")
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        addOption(builder -> builder.name("count")
                .description("Количество предупреждений.")
                .type(ApplicationCommandOption.Type.INTEGER.getValue())
                .minValue(1d)
                .maxValue((double) Integer.MAX_VALUE));
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
            return messageService.err(env, "commands.warn.reason-too-long", AuditLogEntry.MAX_REASON_LENGTH);
        }

        String intervalstr = env.getOption("interval")
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
                        .switchIfEmpty(messageService.err(env, "commands.warn.bot-target").then(Mono.never()))
                        .filter(u -> !u.getId().equals(author.getId()))
                        .switchIfEmpty(messageService.err(env, "commands.warn.self-warn").then(Mono.never()))
                        .filterWhen(u -> u.getBasePermissions()
                                .map(p -> p.equals(PermissionSet.all()) || !p.contains(Permission.ADMINISTRATOR)))
                        .switchIfEmpty(messageService.err(env, "commands.warn.permission-denied").then(Mono.never())))
                .flatMap(TupleUtils.function((config, target) -> {
                    Interval interval = config.warnExpireInterval().orElse(null);
                    if (intervalstr != null) {
                        interval = MessageUtil.parseInterval(intervalstr);

                        if (interval == null) {
                            return messageService.err(env, "commands.common.interval-invalid");
                        }
                    }

                    AllowedMentions allowTarget = AllowedMentions.builder().allowUser(targetId).build();

                    Optional<Instant> endTimestamp = Optional.ofNullable(interval)
                            .map(i -> Instant.now().plus(i));

                    ModerationAction action = ModerationAction.builder()
                            .guildId(guildId.asLong())
                            .adminId(author.getId().asLong())
                            .targetId(targetId.asLong())
                            .type(ModerationAction.Type.warn)
                            .reason(Optional.ofNullable(reason))
                            .endTimestamp(endTimestamp)
                            .build();

                    String byReason = reason != null ? messageService.format(env.context(), "moderation.by-reason", reason) : null;
                    String until = endTimestamp.map(i -> messageService.format(env.context(), "moderation.until",
                            TimestampFormat.LONG_DATE_TIME.format(i)))
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
                            return messageService.text(env, "commands.warn.one.success",
                                            MessageUtil.getUserMention(targetId), jcomp)
                                    .withAllowedMentions(allowTarget)
                                    .and(entityRetriever.save(action));
                        }

                        return messageService.text(env, "commands.warn.many.success", MessageUtil.getUserMention(targetId),
                                        count, messageService.getPluralized(env.context(), "common.plurals.warn", count), jcomp)
                                .withAllowedMentions(allowTarget)
                                .and(entityRetriever.save(action)
                                        .repeat(count - 1));
                    })
                    .then(entityRetriever.moderationActionCountById(ModerationAction.Type.warn, guildId, targetId))
                    .filter(l -> config.thresholdPunishments().map(t -> t.containsKey(l)).orElse(false))
                    .flatMap(l -> {
                        PunishmentSettings sett = config.thresholdPunishments().orElseThrow().get(l);

                        return switch (sett.type()) {
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
                                        .reason(messageService.get(env.context(), "moderation.auto-warn"))
                                        .build();

                                String autoWarnUntil = inst.map(i -> messageService.format(env.context(), "moderation.until",
                                        TimestampFormat.LONG_DATE_TIME.format(i)))
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
                                        .flatMap(c -> c.createMessage(messageService.format(env.context(),
                                                "moderation.auto-warn.text", ajcomp)))
                                        .and(entityRetriever.save(autoWarn));
                            }
                            case mute -> {
                                Optional<Instant> inst = Possible.flatOpt(sett.interval())
                                        .or(config::muteBaseInterval)
                                        .map(i -> Instant.now().plus(i));

                                ModerationAction autoWarn = ModerationAction.builder()
                                        .guildId(guildId.asLong())
                                        .adminId(env.event().getClient().getSelfId().asLong())
                                        .targetId(targetId.asLong())
                                        .type(ModerationAction.Type.mute)
                                        .endTimestamp(inst)
                                        .reason(messageService.get(env.context(), "moderation.auto-mute"))
                                        .build();

                                String autoMuteUntil = inst.map(i -> messageService.format(env.context(), "moderation.until",
                                                TimestampFormat.LONG_DATE_TIME.format(i)))
                                        .orElse(null);
                                StringJoiner ajcomp = new StringJoiner(" ");
                                if (byReason != null) {
                                    ajcomp.add(byReason);
                                }
                                if (autoMuteUntil != null) {
                                    ajcomp.add(autoMuteUntil);
                                }

                                yield target.getPrivateChannel()
                                        .onErrorResume(e -> e instanceof ClientException, e -> Mono.empty())
                                        .flatMap(c -> c.createMessage(messageService.format(env.context(),
                                                "moderation.auto-mute.text", ajcomp)))
                                        .and(target.edit().withReason(reason)
                                                .withCommunicationDisabledUntil(Possible.of(inst)))
                                        .and(entityRetriever.save(autoWarn));
                            }
                            default -> Mono.empty();
                        };
                    });
                }));
    }
}
