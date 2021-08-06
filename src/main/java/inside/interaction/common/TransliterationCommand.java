package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.command.Commands;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@InteractionDiscordCommand(name = "tr", description = "Translating key into transliteration.")
public class TransliterationCommand extends InteractionCommand{

    public TransliterationCommand(){

        addOption(builder -> builder.name("text")
                .description("Translation text")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        String text = env.event().getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(Commands.TransliterationCommand::translit)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> messageService.get(env.context(), "message.placeholder"));

        return env.event().reply(text);
    }
}
