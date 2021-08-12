package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.command.Commands;
import inside.interaction.*;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@InteractionDiscordCommand(name = "tr", description = "Translating key into transliteration.")
public class TransliterationCommand extends BaseInteractionCommand{

    public TransliterationCommand(){

        addOption(builder -> builder.name("text")
                .description("Translation text.")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
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
