package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.command.Commands;
import inside.interaction.*;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@InteractionDiscordCommand(name = "1337", description = "Translate key into leet speak.")
public class LeetSpeakCommand extends BaseInteractionCommand{

    public LeetSpeakCommand(){

        addOption(builder -> builder.name("type")
                .description("Leet speak type")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("English leet")
                        .value("en")
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Russian leet")
                        .value("ru")
                        .build()));

        addOption(builder -> builder.name("text")
                .description("Target text")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        boolean russian = env.event().getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map("ru"::equals)
                .orElse(false);

        String text = env.event().getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> Commands.LeetSpeakCommand.leeted(str, russian))
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> messageService.get(env.context(), "message.placeholder"));

        return env.event().reply(text);
    }
}
