package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.command.common.LeetSpeakCommand;
import inside.interaction.*;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@InteractionDiscordCommand(name = "1337", description = "Translate text into leet speak.")
public class InteractionLeetSpeakCommand extends BaseInteractionCommand{

    public InteractionLeetSpeakCommand(){

        addOption(builder -> builder.name("type")
                .description("Leet speak type.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("English leet.")
                        .value("en")
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Russian leet.")
                        .value("ru")
                        .build()));

        addOption(builder -> builder.name("text")
                .description("Text.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        boolean russian = env.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map("ru"::equals)
                .orElse(false);

        String text = env.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> LeetSpeakCommand.leeted(str, russian))
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> messageService.get(env.context(), "message.placeholder"));

        return env.event().reply(text);
    }
}
