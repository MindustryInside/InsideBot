package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
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
import inside.interaction.util.MessagePaginator;
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
        addSubcommand(new MuteRoleSubcommand(this));
        addSubcommand(new MuteRoleResetSubcommand(this));
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
                    .switchIfEmpty(messageService.err(env, "Сначала измените настройки модерации").then(Mono.never()))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "Функции модерации: **%s**",
                                    config.enabled() ? "включены" : "выключены").then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "Функции модерации: **%s**",
                                    state ? "включены" : "выключены")
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
                            .switchIfEmpty(messageService.text(env, "Текущая базовая длительность предупреждений: **%s**",
                                    config.warnExpireInterval().map(formatDuration).orElse("не установлена"))
                                    .then(Mono.never()))
                            .flatMap(str -> {
                                boolean delete = clearAliases.contains(str.toLowerCase(Locale.ROOT));
                                Interval inter = delete ? null : MessageUtil.parseInterval(str);
                                if (!delete && inter == null) {
                                    return messageService.err(env, "Неправильный формат длительности");
                                }

                                return messageService.text(env, delete ? "Базовая длительность предупреждений сброшена"
                                                : "Базовая длительность предупреждений обновлена: **%s**",
                                                Optional.ofNullable(inter).map(formatDuration).orElse("не установлена"))
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
                            .switchIfEmpty(messageService.text(env, "Текущая базовая длительность мута: **%s**",
                                    config.muteBaseInterval().map(formatDuration).orElse("не установлена"))
                                    .then(Mono.never()))
                            .flatMap(str -> {
                                boolean delete = clearAliases.contains(str.toLowerCase(Locale.ROOT));
                                Interval inter = delete ? null : MessageUtil.parseInterval(str);
                                if (!delete && inter == null) {
                                    return messageService.err(env, "Неправильный формат длительности");
                                }

                                return messageService.text(env, delete ?
                                                "Базовая длительность мута сброшена"
                                                : "Базовая длительность мута обновлена: **%s**",
                                                Optional.ofNullable(inter).map(formatDuration).orElse("не установлена"))
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
                                return messageService.err(env, "Такая роль уже находится в списке");
                            }

                            return messageService.text(env, "Роль успешно добавлена в список: %s",
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
                                return messageService.err(env, "Такой роли нет в списке");
                            }

                            return messageService.text(env, "Роль успешно удалена из списка: %s",
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
                        .switchIfEmpty(messageService.err(env, "Список ролей пуст").then(Mono.never()))
                        .flatMap(config -> messageService.text(env, "Список ролей-админов отчищен")
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
                        .switchIfEmpty(messageService.err(env, "Список ролей пуст").then(Mono.never()))
                        .flatMap(list -> messageService.text(env, "Текущий список ролей: **%s**",
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
                    return messageService.err(env, "Неправильный формат длительности");
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
                                return messageService.err(env, "Такое авто-наказание уже есть");
                            }

                            return messageService.text(env, "Авто-наказание успешно создано: %s", formatter.apply(punishmentSettings))
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
                                return messageService.err(env, "Такого авто-наказания нет");
                            }

                            return messageService.text(env, "Авто-наказание успешно удалено: %s", formatter.apply(remove))
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
                        .switchIfEmpty(messageService.err(env, "Список авто-наказаний пуст").then(Mono.never()))
                        .flatMap(config -> messageService.text(env, "Список авто-наказаний очищен")
                                .and(owner.entityRetriever.save(config.withThresholdPunishments(Optional.empty()))));
            }
        }

        @Subcommand(name = "list", description = "Отобразить список авто-наказаний.")
        protected static class ListSubcommand extends InteractionSubcommand<ThresholdPunishmentsSubcommandGroup> {

            private static final int PER_PAGE = 10;

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

                Function<MessagePaginator.Page, ? extends Mono<MessageCreateSpec>> paginator = page ->
                        owner.entityRetriever.getModerationConfigById(guildId)
                                .flatMapMany(config -> Mono.justOrEmpty(config.thresholdPunishments())
                                        .filter(l -> !l.isEmpty())
                                        .flatMapIterable(Map::entrySet))
                                .switchIfEmpty(messageService.err(env, "Список авто-наказаний пуст").then(Mono.never()))
                                .sort(Comparator.comparingLong(Map.Entry::getKey))
                                .skip(page.getPage() * PER_PAGE).take(PER_PAGE, true)
                                .map(entry -> formatter.apply(entry.getKey(), entry.getValue()))
                                .collect(Collectors.joining())
                                .map(str -> MessageCreateSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Текущий список авто-наказаний")
                                                .description(str)
                                                .color(env.configuration().discord().embedColor())
                                                .footer(String.format("Страница %s/%s", page.getPage() + 1, page.getPageCount()), null)
                                                .build())
                                        .components(page.getItemsCount() > PER_PAGE
                                                ? Possible.of(List.of(ActionRow.of(
                                                page.previousButton(id -> Button.primary(id, "Предыдущая Страница")),
                                                page.nextButton(id -> Button.primary(id, "Следующая Страница")))))
                                                : Possible.absent())
                                        .build());

                return owner.entityRetriever.getModerationConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                        .flatMap(config -> Mono.justOrEmpty(config.thresholdPunishments()).filter(l -> !l.isEmpty()))
                        .switchIfEmpty(messageService.err(env, "Список авто-наказаний пуст").then(Mono.never()))
                        .flatMap(map -> MessagePaginator.paginate(env, map.size(), PER_PAGE, paginator));
            }
        }
    }

    @Subcommand(name = "mute-role", description = "Настроить роль мута. При отсутствии используется таймаут")
    protected static class MuteRoleSubcommand extends InteractionSubcommand<AdminConfigCommand> {

        protected MuteRoleSubcommand(AdminConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новая роль мута.")
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getModerationConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                                    .map(Snowflake::asLong))
                            .switchIfEmpty(messageService.text(env, "Текущая роль мута: **%s**",
                                    config.muteRoleId().map(MessageUtil::getRoleMention).orElse("Не установлена"))
                                    .then(Mono.never()))
                            .flatMap(roleId -> messageService.text(env, "Роль мута обновлена: %s",
                                            MessageUtil.getRoleMention(roleId))
                                    .and(owner.entityRetriever.save(config.withMuteRoleId(roleId)))));
        }
    }

    @Subcommand(name = "mute-role-reset", description = "Сбросить роль мута.")
    protected static class MuteRoleResetSubcommand extends InteractionSubcommand<AdminConfigCommand> {

        protected MuteRoleResetSubcommand(AdminConfigCommand owner) {
            super(owner);
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getModerationConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createModerationConfigById(guildId))
                    .flatMap(config -> messageService.text(env, "Роль мута успешно сброшена")
                            .and(owner.entityRetriever.save(config.withMuteRoleId(Optional.empty()))));
        }
    }
}
