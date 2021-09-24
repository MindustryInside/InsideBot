package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import inside.interaction.*;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;
import static reactor.function.TupleUtils.function;

@InteractionDiscordCommand(name = "locale", description = "Configure bot locale.")
public class InteractionLocaleCommand extends SettingsCommand{

    protected InteractionLocaleCommand(){

        addOption(builder -> builder.name("value")
                .description("New locale.")
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){

        Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

        return entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .zipWhen(guildConfig -> Mono.justOrEmpty(env.getOption("value")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString))
                        .switchIfEmpty(messageService.text(env.event(), "command.settings.locale.current",
                                guildConfig.locale().getDisplayName()).then(Mono.never())))
                .flatMap(function((guildConfig, value) -> {
                    Locale locale = messageService.getLocale(value).orElse(null);
                    if(locale == null){
                        String all = messageService.getSupportedLocales().values().stream()
                                .map(locale1 -> String.format("%s (`%s`)", locale1.getDisplayName(), locale1))
                                .collect(Collectors.joining(", "));

                        return messageService.text(env.event(), "command.settings.locale.all", all);
                    }

                    guildConfig.locale(locale);
                    return Mono.deferContextual(ctx -> messageService.text(env.event(), "command.settings.locale.update",
                                    ctx.<Locale>get(KEY_LOCALE).getDisplayName()))
                            .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale))
                            .and(entityRetriever.save(guildConfig));
                }));
    }
}
