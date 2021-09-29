package inside.command.settings;

import discord4j.core.object.entity.Member;
import inside.command.CommandCategory;
import inside.command.model.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

@DiscordCommand(key = "prefix", params = "command.settings.prefix.params", description = "command.settings.prefix.description",
        category = CommandCategory.owner)
public class PrefixCommand extends OwnerCommand{

    private static final Pattern modePattern = Pattern.compile("^(add|remove|clear)$", Pattern.CASE_INSENSITIVE);

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member member = env.member();

        String mode = interaction.getOption(0)
                .flatMap(CommandOption::getChoice)
                .map(OptionValue::asString)
                .filter(s -> modePattern.matcher(s).matches())
                .orElse(null);

        String value = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElse(null);

        return entityRetriever.getGuildConfigById(member.getGuildId())
                .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                .flatMap(guildConfig -> Mono.defer(() -> {
                    List<String> prefixes = guildConfig.prefixes();
                    if(mode == null){
                        return messageService.text(env, "command.settings.prefix.current",
                                String.join(", ", prefixes));
                    }else if(mode.equalsIgnoreCase("add")){
                        if(value == null){
                            return messageService.err(env, "command.settings.prefix-absent");
                        }
                        prefixes.add(value);
                        return messageService.text(env, "command.settings.added", value);
                    }else if(mode.equalsIgnoreCase("remove")){
                        if(value == null){
                            return messageService.err(env, "command.settings.prefix-absent");
                        }
                        prefixes.remove(value);
                        return messageService.text(env, "command.settings.removed", value);
                    }else{ // clear
                        // ignore value, it doesn't matter
                        prefixes.clear();
                        return messageService.text(env, "command.settings.prefix.clear");
                    }
                }).and(entityRetriever.save(guildConfig)));
    }
}
