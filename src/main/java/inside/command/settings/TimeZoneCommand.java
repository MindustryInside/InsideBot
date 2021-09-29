package inside.command.settings;

import discord4j.core.object.entity.Member;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.util.*;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.*;

import static inside.util.ContextUtil.KEY_TIMEZONE;

@DiscordCommand(key = "timezone", params = "command.settings.timezone.params", description = "command.settings.timezone.description",
        category = CommandCategory.owner)
public class TimeZoneCommand extends OwnerCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member member = env.member();

        boolean present = interaction.getOption(0).isPresent();

        ZoneId timeZone = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .flatMap(str -> Try.ofCallable(() -> ZoneId.of(str)).toOptional())
                .orElse(null);

        String str = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElse("");

        return entityRetriever.getGuildConfigById(member.getGuildId())
                .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                .filter(ignored -> present)
                .switchIfEmpty(messageService.text(env, "command.settings.timezone.current",
                        env.context().<Locale>get(KEY_TIMEZONE)).then(Mono.empty()))
                .flatMap(guildConfig -> Mono.defer(() -> {
                    if(timeZone == null){
                        return ZoneId.getAvailableZoneIds().stream()
                                .min(Comparator.comparingInt(s -> Strings.damerauLevenshtein(s, str)))
                                .map(s -> messageService.err(env, "command.settings.timezone.unknown.suggest", s))
                                .orElse(messageService.err(env, "command.settings.timezone.unknown"));
                    }

                    guildConfig.timeZone(timeZone);
                    return Mono.deferContextual(ctx -> messageService.text(env,
                                    "command.settings.timezone.update", ctx.<Locale>get(KEY_TIMEZONE)))
                            .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone))
                            .and(entityRetriever.save(guildConfig));
                }));
    }
}
