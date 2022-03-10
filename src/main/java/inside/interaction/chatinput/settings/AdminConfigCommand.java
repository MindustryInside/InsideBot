package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.possible.Possible;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.data.entity.PunishmentSettings;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.service.MessageService;
import inside.util.DurationFormat;
import inside.util.MessageUtil;
import io.r2dbc.postgresql.codec.Interval;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ChatInputCommand(name = "admin-config", description = "Настройки модерации.", permissions = PermissionCategory.ADMIN)
public class AdminConfigCommand extends ConfigOwnerCommand {

    private static final long MAX_INT53 = 9007199254740991L;
    private static final Set<String> clearAliases = Set.of("clear", "clean", "delete", "remove", "удалить", "отчистить");

    public AdminConfigCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new EnableSubcommand(this));
        addSubcommand(new WarnExpireIntervalSubcommand(this));
        addSubcommand(new MuteBaseIntervalSubcommand(this));
        addSubcommand(new AdminRolesSubcommandGroup(this));
        addSubcommand(new ThresholdPunishmentsSubcommandGroup(this));
    }

    @Subcommand(name = "enable", description = "Включить модерацию.")
    protected static class EnableSubcommand extends InteractionSubcommand<AdminConfigCommand> {

        protected EnableSubcommand(AdminConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новое состояние.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getModerationConfigById(guildId)
                    .switchIfEmpty(messageService.err(env, "commands.moderation-config.enable.unconfigured").then(Mono.never()))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "commands.moderation-config.enable.format",
                                    messageService.getBool(env.context(), "commands.moderation-config.enable", config.enabled())).then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "commands.moderation-config.enable.format",
                                            messageService.getBool(env.context(), "commands.moderation-config.enable", state))
                                    .and(owner.entityRetriever.save(config.withEnabled(state)))));
        }
    }

    @Subcommand(name = "warn-expire-interval", description = "Настроить базовое время жизни предупреждения.")
    protected static class WarnExpireIntervalSubcommand extends InteractionSubcommand<AdminConfigCommand> {

        protected WarnExpireIntervalSubcommand(AdminConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новое базовое время жизни предупреждения или 'отчистить'. (в формате 1д 3ч 44мин)")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Function<TemporalAmount, String> formatDuration = DurationFormat.wordBased()::format;

            return owner.entityRetriever.getModerationConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "commands.moderation-config.warn-expire-interval.current",
                                    config.warnExpireInterval().map(formatDuration)
                                            .orElseGet(() -> messageService.get(env.context(),
                                                    "commands.moderation-config.interval-absent"))).then(Mono.never()))
                            .flatMap(str -> {
                                boolean delete = clearAliases.contains(str.toLowerCase(Locale.ROOT));
                                Interval inter = delete ? null : MessageUtil.parseInterval(str);
                                if (!delete && inter == null) {
                                    return messageService.err(env, "commands.common.interval-invalid");
                                }

                                return messageService.text(env, "commands.moderation-config.warn-expire-interval.update",
                                                Optional.ofNullable(inter).map(formatDuration)
                                                        .orElseGet(() -> messageService.get(env.context(),
                                                                "commands.moderation-config.interval-absent")))
                                        .and(owner.entityRetriever.save(config.withWarnExpireInterval(
                                                Optional.ofNullable(inter))));
                            }));
        }
    }

    @Subcommand(name = "mute-base-interval", description = "Настроить базовую длительность мута.")
    protected static class MuteBaseIntervalSubcommand extends InteractionSubcommand<AdminConfigCommand> {

        protected MuteBaseIntervalSubcommand(AdminConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новая базовая длительность мута или 'отчистить'. (в формате 1д 3ч 44мин)")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Function<TemporalAmount, String> formatDuration = DurationFormat.wordBased()::format;

            return owner.entityRetriever.getModerationConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "commands.moderation-config.mute-base-interval.current",
                                    config.muteBaseInterval().map(formatDuration)
                                            .orElseGet(() -> messageService.get(env.context(),
                                                    "commands.moderation-config.interval-absent"))).then(Mono.never()))
                            .flatMap(str -> {
                                boolean delete = clearAliases.contains(str.toLowerCase(Locale.ROOT));
                                Interval inter = delete ? null : MessageUtil.parseInterval(str);
                                if (!delete && inter == null) {
                                    return messageService.err(env, "commands.common.interval-invalid");
                                }

                                return messageService.text(env, "commands.moderation-config.mute-base-interval.update",
                                                Optional.ofNullable(inter).map(formatDuration)
                                                        .orElseGet(() -> messageService.get(env.context(),
                                                                "commands.moderation-config.interval-absent")))
                                        .and(owner.entityRetriever.save(config.withMuteBaseInterval(
                                                Optional.ofNullable(inter))));
                            }));
        }
    }

    @SubcommandGroup(name = "admin-roles", description = "Настроить админ-роли.")
    protected static class AdminRolesSubcommandGroup extends ConfigOwnerCommand {

        protected AdminRolesSubcommandGroup(AdminConfigCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand(name = "add", description = "Добавить роль в список.")
        protected static class AddSubcommand extends InteractionSubcommand<AdminRolesSubcommandGroup> {

            protected AddSubcommand(AdminRolesSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Роль, которую нужно добавить в список.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.ROLE.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                Id roleId = env.getOption("value")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                        .map(id -> Id.of(id.asLong()))
                        .orElseThrow();

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> {
                            var set = new HashSet<>(config.adminRoleIds().orElse(Set.of()));
                            boolean add = set.add(roleId);
                            if (!add) {
                                return messageService.err(env, "commands.moderation-config.admin-roles.add.already-exists");
                            }

                            return messageService.text(env, "commands.moderation-config.admin-roles.add.success",
                                            MessageUtil.getRoleMention(roleId.asLong()))
                                    .and(owner.entityRetriever.save(config.withAdminRoleIds(set)));
                        });
            }
        }

        @Subcommand(name = "remove", description = "Удалить роль из списка.")
        protected static class RemoveSubcommand extends InteractionSubcommand<AdminRolesSubcommandGroup> {

            protected RemoveSubcommand(AdminRolesSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Роль, которую нужно удалить из списка.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.ROLE.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                Id roleId = env.getOption("value")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                        .map(id -> Id.of(id.asLong()))
                        .orElseThrow();

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> {
                            var set = new HashSet<>(config.adminRoleIds().orElse(Set.of()));
                            boolean remove = set.remove(roleId);
                            if (!remove) {
                                return messageService.err(env, "commands.moderation-config.admin-roles.remove.unknown");
                            }

                            return messageService.text(env, "commands.moderation-config.admin-roles.remove.success",
                                            MessageUtil.getRoleMention(roleId.asLong()))
                                    .and(owner.entityRetriever.save(config.withAdminRoleIds(set)));
                        });
            }
        }

        @Subcommand(name = "clear", description = "Отчистить список ролей.")
        protected static class ClearSubcommand extends InteractionSubcommand<AdminRolesSubcommandGroup> {

            protected ClearSubcommand(AdminRolesSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .filter(config -> config.adminRoleIds().map(l -> !l.isEmpty()).orElse(false))
                        .switchIfEmpty(messageService.err(env, "commands.moderation-config.admin-roles.list.empty").then(Mono.never()))
                        .flatMap(config -> messageService.text(env, "commands.moderation-config.admin-roles.clear.success")
                                .and(owner.entityRetriever.save(config.withAdminRoleIds(Optional.empty()))));
            }
        }

        @Subcommand(name = "list", description = "Отобразить список ролей.")
        protected static class ListSubcommand extends InteractionSubcommand<AdminRolesSubcommandGroup> {

            protected ListSubcommand(AdminRolesSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> Mono.justOrEmpty(config.adminRoleIds()).filter(l -> !l.isEmpty()))
                        .switchIfEmpty(messageService.err(env, "commands.moderation-config.admin-roles.list.empty").then(Mono.never()))
                        .flatMap(list -> messageService.text(env, "commands.moderation-config.admin-roles.list.format",
                                list.stream().map(i -> MessageUtil.getRoleMention(i.asLong()))
                                        .collect(Collectors.joining(", "))));
            }
        }
    }

    @SubcommandGroup(name = "threshold-punishment", description = "Настроить админ-роли.")
    protected static class ThresholdPunishmentsSubcommandGroup extends ConfigOwnerCommand {

        protected ThresholdPunishmentsSubcommandGroup(AdminConfigCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand(name = "add", description = "Добавить новое авто-наказание в список.")
        protected static class AddSubcommand extends InteractionSubcommand<ThresholdPunishmentsSubcommandGroup> {

            protected AddSubcommand(ThresholdPunishmentsSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("warn-number")
                        .description("Номер предупреждения.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(1d)
                        .maxValue((double) MAX_INT53));

                addOption(builder -> builder.name("type")
                        .description("Тип наказания.")
                        .required(true)
                        .choices(Stream.of(ModerationAction.Type.values())
                                .map(type -> ApplicationCommandOptionChoiceData.builder()
                                        .name(type.name()) // TODO: сюда перевод
                                        .value(type.name())
                                        .build())
                                .collect(Collectors.toList()))
                        .type(ApplicationCommandOption.Type.STRING.getValue()));

                addOption(builder -> builder.name("interval")
                        .description("Длительность наказания.")
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                long warnNumber = env.getOption("warn-number")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .orElseThrow();

                ModerationAction.Type type = env.getOption("type")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .map(ModerationAction.Type::valueOf)
                        .orElseThrow();

                String intervalstr = env.getOption("interval")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse(null);

                Possible<Optional<Interval>> inter = intervalstr != null
                        ? Possible.of(Optional.ofNullable(MessageUtil.parseInterval(intervalstr)))
                        : Possible.absent();

                if (!inter.isAbsent() && inter.get().isEmpty()) {
                    return messageService.err(env, "commands.common.interval-invalid");
                }

                PunishmentSettings punishmentSettings = PunishmentSettings.builder()
                        .type(type)
                        .interval(inter)
                        .build();

                Function<TemporalAmount, String> formatDuration = DurationFormat.wordBased()::format;

                Function<PunishmentSettings, String> formatter = sett ->
                        "**%s** - %s%s%n".formatted(warnNumber, messageService.getEnum(env.context(), sett.type()),
                                Possible.flatOpt(sett.interval()).map(formatDuration)
                                        .map(str -> " (" + str + ")").orElse(""));

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> {
                            var map = new HashMap<>(config.thresholdPunishments().orElse(Map.of()));
                            boolean add = map.put(warnNumber, punishmentSettings) == null;
                            if (!add) {
                                return messageService.err(env, "commands.moderation-config.threshold-punishment.add.already-exists");
                            }

                            return messageService.text(env, "commands.moderation-config.threshold-punishment.add.success",
                                            formatter.apply(punishmentSettings))
                                    .and(owner.entityRetriever.save(config.withThresholdPunishments(map)));
                        });
            }
        }

        @Subcommand(name = "remove", description = "Удалить авто-наказание из списка.")
        protected static class RemoveSubcommand extends InteractionSubcommand<ThresholdPunishmentsSubcommandGroup> {

            protected RemoveSubcommand(ThresholdPunishmentsSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("warn-number")
                        .description("Номер предупреждения.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(1d)
                        .maxValue((double) MAX_INT53));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                long warnNumber = env.getOption("warn-number")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .orElseThrow();

                Function<TemporalAmount, String> formatDuration = DurationFormat.wordBased()::format;

                Function<PunishmentSettings, String> formatter = sett ->
                        "**%s** - %s%s%n".formatted(warnNumber, messageService.getEnum(env.context(), sett.type()),
                                Possible.flatOpt(sett.interval()).map(formatDuration)
                                        .map(str -> " (" + str + ")").orElse(""));

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> {
                            var map = new HashMap<>(config.thresholdPunishments().orElse(Map.of()));
                            PunishmentSettings remove = map.remove(warnNumber);
                            if (remove == null) {
                                return messageService.err(env, "commands.moderation-config.threshold-punishment.remove.unknown");
                            }

                            return messageService.text(env, "commands.moderation-config.threshold-punishment.remove.success",
                                            formatter.apply(remove))
                                    .and(owner.entityRetriever.save(config.withThresholdPunishments(map)));
                        });
            }
        }

        @Subcommand(name = "clear", description = "Отчистить список авто-наказаний.")
        protected static class ClearSubcommand extends InteractionSubcommand<ThresholdPunishmentsSubcommandGroup> {

            protected ClearSubcommand(ThresholdPunishmentsSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .filter(config -> config.thresholdPunishments().map(l -> !l.isEmpty()).orElse(false))
                        .switchIfEmpty(messageService.err(env, "commands.moderation-config.threshold-punishment.list.empty").then(Mono.never()))
                        .flatMap(config -> messageService.text(env, "commands.moderation-config.threshold-punishment.clear.success")
                                .and(owner.entityRetriever.save(config.withThresholdPunishments(Optional.empty()))));
            }
        }

        @Subcommand(name = "list", description = "Отобразить список авто-наказаний.")
        protected static class ListSubcommand extends InteractionSubcommand<ThresholdPunishmentsSubcommandGroup> {

            protected ListSubcommand(ThresholdPunishmentsSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                Function<TemporalAmount, String> formatDuration = DurationFormat.wordBased()::format;

                BiFunction<Long, PunishmentSettings, String> formatter = (warnNumber, sett) ->
                        "**%s** - %s%s%n".formatted(warnNumber, messageService.getEnum(env.context(), sett.type()),
                                Possible.flatOpt(sett.interval()).map(formatDuration)
                                        .map(str -> " (" + str + ")").orElse(""));

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> Mono.justOrEmpty(config.thresholdPunishments()).filter(l -> !l.isEmpty()))
                        .switchIfEmpty(messageService.err(env, "commands.moderation-config.threshold-punishment.list.empty").then(Mono.never()))
                        .flatMap(map -> messageService.infoTitled(env, "commands.moderation-config.threshold-punishment.list.title",
                                map.entrySet().stream()
                                        .map(e -> formatter.apply(e.getKey(), e.getValue()))
                                        .collect(Collectors.joining())));
            }
        }
    }
}
