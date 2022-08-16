package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.service.MessageService;
import inside.util.ResourceMessageSource;
import inside.util.Strings;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ChatInputCommand(value = "config")// = "Общие настройки.", permissions = PermissionCategory.ADMIN)
public class GuildConfigCommand extends ConfigOwnerCommand {

    public GuildConfigCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new LocaleSubcommand(this));
        addSubcommand(new TimeZoneSubcommand(this));
        addSubcommand(new PrefixesSubcommandGroup(this));
    }

    @Subcommand(value = "locale")// = "Настроить язык.")
    protected static class LocaleSubcommand extends InteractionSubcommand<GuildConfigCommand> {

        protected LocaleSubcommand(GuildConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новый язык.")
                    .choices(ResourceMessageSource.supportedLocaled.stream()
                            .map(l -> ApplicationCommandOptionChoiceData.builder()
                                    .name(l.getDisplayName(l))
                                    .value(l.getLanguage())
                                    .build())
                            .collect(Collectors.toList()))
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .map(Locale::new)) // безопасно
                            .switchIfEmpty(messageService.text(env, "Текущий язык бота: **%s**",
                                    config.locale().getDisplayName(config.locale())).then(Mono.never()))
                            .filter(l -> !l.equals(config.locale()))
                            .switchIfEmpty(messageService.text(env, "Язык бота не изменён").then(Mono.never()))
                            .flatMap(locale -> messageService.text(env, "Язык изменён на: **%s**", locale.getDisplayName(locale))
                                    .and(owner.entityRetriever.save(config.withLocale(locale)))));
        }
    }

    @Subcommand(value = "timezone")// = "Настроить временную зону.")
    protected static class TimeZoneSubcommand extends InteractionSubcommand<GuildConfigCommand> {

        protected TimeZoneSubcommand(GuildConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новая временная зона. (в формате Europe/Moscow или +3)")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "Текущий часовой пояс бота: **%s**",
                                    config.timezone()).then(Mono.never()))
                            .flatMap(tz -> Mono.fromCallable(() -> ZoneId.of(tz))
                                    .flatMap(id -> messageService.text(env, "Часовой пояс изменён на: **%s**", id)
                                            .and(owner.entityRetriever.save(config.withTimezone(id))))
                                    .onErrorResume(e -> ZoneId.getAvailableZoneIds().stream()
                                            .min(Comparator.comparingInt(s -> Strings.damerauLevenshtein(s, tz)))
                                            .map(s -> messageService.err(env, "Часовой пояс не найден. Может вы имели в виду \"%s\"?", s))
                                            .orElseGet(() -> messageService.err(env, "Часовой пояс не найден")))));
        }
    }

    @SubcommandGroup(value = "prefixes")// = "Настроить префиксы.")
    protected static class PrefixesSubcommandGroup extends ConfigOwnerCommand {

        protected PrefixesSubcommandGroup(GuildConfigCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand(value = "add")// = "Добавить эмодзи в список.")
        protected static class AddSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected AddSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Новый префикс.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                String prefix = env.getOption("value")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElseThrow();

                return owner.entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                        .flatMap(config -> {
                            var set = new HashSet<>(config.prefixes());
                            boolean add = set.add(prefix);
                            if (!add) {
                                return messageService.err(env, "Такой префикс уже находится в списке.");
                            }

                            return messageService.text(env, "Префикс добавлен в список: **%s**", prefix)
                                    .and(owner.entityRetriever.save(config.withPrefixes(set)));
                        });
            }
        }

        @Subcommand(value = "remove")// = "Удалить эмодзи из списка.")
        protected static class RemoveSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected RemoveSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Префикс, который нужно удалить.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                String prefix = env.getOption("value")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElseThrow();

                return owner.entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                        .flatMap(config -> {
                            var set = new HashSet<>(config.prefixes());
                            boolean add = set.add(prefix);
                            if (!add) {
                                return messageService.err(env, "Такого префикса нет в списке.");
                            }

                            return messageService.text(env, "Префикс удалён из списка: **%s**", prefix)
                                    .and(owner.entityRetriever.save(config.withPrefixes(set)));
                        });
            }
        }

        @Subcommand(value = "clear")// = "Отчистить список эмодзи.")
        protected static class ClearSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected ClearSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                        .flatMap(config -> config.prefixes().isEmpty()
                                ? messageService.err(env, "Список префиксов пуст")
                                : messageService.text(env, "Список префиксов очищен")
                                .and(owner.entityRetriever.save(config.withPrefixes(List.of()))));
            }
        }

        @Subcommand(value = "list")// = "Отобразить список эмодзи.")
        protected static class ListSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected ListSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createGuildConfig(guildId))
                        .flatMap(config -> config.prefixes().isEmpty()
                                ? messageService.err(env, "Список префиксов пуст")
                                : messageService.text(env, "Текущий список префиксов: %s",
                                String.join(", ", config.prefixes())));
            }
        }
    }
}
