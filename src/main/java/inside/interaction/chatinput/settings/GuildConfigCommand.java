package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.interaction.chatinput.InteractionSubcommandGroup;
import inside.service.MessageService;
import inside.util.Strings;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@ChatInputCommand(value = "config", permissions = PermissionCategory.ADMIN)// = "Общие настройки."
public class GuildConfigCommand extends InteractionSubcommandGroup {

    public GuildConfigCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new LocaleSubcommand(this));
        addSubcommand(new TimeZoneSubcommand(this));
        addSubcommand(new PrefixesSubcommandGroup(this));
    }

    @Subcommand("locale")// = "Настроить язык.")
    @Option(name = "value", type = Type.STRING, choices = {
            @Choice(name = "Русский", value = "ru"), // NOTE: инициализация не очень
            @Choice(name = "English", value = "en")
    })
    protected static class LocaleSubcommand extends InteractionSubcommand<GuildConfigCommand> {

        protected LocaleSubcommand(GuildConfigCommand owner) {
            super(owner);
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

    @Subcommand("timezone")// = "Настроить временную зону.")
    @Option(name = "value", type = Type.STRING)
    protected static class TimeZoneSubcommand extends InteractionSubcommand<GuildConfigCommand> {

        protected TimeZoneSubcommand(GuildConfigCommand owner) {
            super(owner);
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

    @SubcommandGroup("prefixes")// = "Настроить префиксы.")
    protected static class PrefixesSubcommandGroup extends InteractionSubcommandGroup {

        protected PrefixesSubcommandGroup(GuildConfigCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand("add")// = "Добавить эмодзи в список.")
        @Option(name = "value", type = Type.STRING, required = true)
        protected static class AddSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected AddSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);
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

        @Subcommand("remove")// = "Удалить эмодзи из списка.")
        @Option(name = "value", type = Type.STRING, required = true)
        protected static class RemoveSubcommand extends InteractionSubcommand<PrefixesSubcommandGroup> {

            protected RemoveSubcommand(PrefixesSubcommandGroup owner) {
                super(owner);
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

        @Subcommand("clear")// = "Отчистить список эмодзи.")
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

        @Subcommand("list")// = "Отобразить список эмодзи.")
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
