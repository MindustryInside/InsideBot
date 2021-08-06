package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import inside.util.*;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.*;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

@InteractionDiscordCommand(name = "timezone", description = "Configure bot time zone.")
public class TimeZoneCommand extends SettingsCommand{

    public TimeZoneCommand(){

        addOption(builder -> builder.name("value")
                .description("New time zone.")
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){

        Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

        return entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .zipWhen(guildConfig -> Mono.justOrEmpty(env.getOption("value")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString))
                        .switchIfEmpty(messageService.text(env.event(), "command.settings.timezone.current",
                                guildConfig.timeZone()).then(Mono.never())))
                .flatMap(function((guildConfig, value) -> {
                    ZoneId timeZone = Try.ofCallable(() -> ZoneId.of(value)).orElse(null);
                    if(timeZone == null){
                        return ZoneId.getAvailableZoneIds().stream()
                                .min(Comparator.comparingInt(s -> Strings.levenshtein(s, value)))
                                .map(s -> messageService.err(env.event(), "command.settings.timezone.unknown.suggest", s))
                                .orElse(messageService.err(env.event(), "command.settings.timezone.unknown"))
                                .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                    }

                    guildConfig.timeZone(timeZone);
                    return Mono.deferContextual(ctx -> messageService.text(env.event(),
                                    "command.settings.timezone.update", ctx.<Locale>get(KEY_TIMEZONE)))
                            .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone))
                            .and(entityRetriever.save(guildConfig));
                }));
    }
}
