package inside.command.settings;

import discord4j.core.object.entity.Member;
import inside.command.CommandCategory;
import inside.command.model.*;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;

@DiscordCommand(key = "locale", params = "command.settings.locale.params", description = "command.settings.locale.description",
        category = CommandCategory.owner)
public class LocaleCommand extends OwnerCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member member = env.member();

        boolean present = interaction.getOption(0).isPresent();

        Locale locale = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .flatMap(messageService::getLocale)
                .orElse(null);

        return entityRetriever.getGuildConfigById(member.getGuildId())
                .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                .filter(guildConfig -> present)
                .switchIfEmpty(messageService.text(env, "command.settings.locale.current",
                        env.context().<Locale>get(KEY_LOCALE).getDisplayName()).then(Mono.empty()))
                .flatMap(guildConfig -> {
                    if(locale == null){
                        String all = messageService.getSupportedLocales().values().stream()
                                .map(locale1 -> String.format("%s (`%s`)", locale1.getDisplayName(), locale1))
                                .collect(Collectors.joining(", "));

                        return messageService.text(env, "command.settings.locale.all", all);
                    }

                    guildConfig.locale(locale);
                    return Mono.deferContextual(ctx -> messageService.text(env, "command.settings.locale.update",
                                    ctx.<Locale>get(KEY_LOCALE).getDisplayName()))
                            .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale))
                            .and(entityRetriever.save(guildConfig));
                })
                .then();
    }
}
