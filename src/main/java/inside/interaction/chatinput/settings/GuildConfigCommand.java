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
import inside.interaction.chatinput.InteractionSubcommand;
import inside.service.MessageService;
import inside.util.Strings;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

@ChatInputCommand(name = "config", description = "Общие настройки.")
public class GuildConfigCommand extends ConfigOwnerCommand {

    public GuildConfigCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new LocaleSubcommand(this));
        addSubcommand(new TimeZoneSubcommand(this));
    }

    @Subcommand(name = "locale", description = "Настроить язык.")
    protected static class LocaleSubcommand extends InteractionSubcommand<GuildConfigCommand> {

        protected LocaleSubcommand(GuildConfigCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новый язык.")
                    .choices(MessageService.supportedLocaled.stream()
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
                            .switchIfEmpty(messageService.text(env, "commands.config.locale.current",
                                    config.locale().getDisplayName(config.locale())).then(Mono.never()))
                            .flatMap(locale -> messageService.text(env, "commands.config.locale.update", locale.getDisplayName(locale))
                                    .and(owner.entityRetriever.save(config.withLocale(locale)))));
        }
    }

    @Subcommand(name = "timezone", description = "Настроить временную зону.")
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
                            .switchIfEmpty(messageService.text(env, "commands.config.timezone.current",
                                    config.timezone()).then(Mono.never()))
                            .flatMap(tz -> {
                                try {
                                    ZoneId timezone = ZoneId.of(tz);
                                    return messageService.text(env, "commands.config.timezone.update", timezone)
                                            .and(owner.entityRetriever.save(config.withTimezone(timezone)));
                                } catch (Throwable t) {
                                    return ZoneId.getAvailableZoneIds().stream()
                                            .min(Comparator.comparingInt(s -> Strings.damerauLevenshtein(s, tz)))
                                            .map(s -> messageService.err(env, "commands.config.timezone.unknown.suggest", s))
                                            .orElseGet(() -> messageService.err(env, "commands.config.timezone.unknown"));
                                }
                            }));
        }
    }
}
