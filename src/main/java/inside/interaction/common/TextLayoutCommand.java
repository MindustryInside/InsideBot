package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.command.Commands;
import inside.interaction.*;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "r", description = "Change text layout.")
public class TextLayoutCommand extends BaseInteractionCommand{

    public TextLayoutCommand(){

        addOption(builder -> builder.name("type")
                .description("Text layout type.")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("English layout.")
                        .value("en")
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Russian layout.")
                        .value("ru")
                        .build()));

        addOption(builder -> builder.name("text")
                .description("Text.")
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
                .map(str -> russian
                        ? Commands.TextLayoutCommand.text2eng(str)
                        : Commands.TextLayoutCommand.text2rus(str))
                .orElseGet(() -> messageService.get(env.context(), "message.placeholder"));

        return env.event().reply(text);
    }
}
