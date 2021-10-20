package inside.interaction.chatinput.common;

import discord4j.core.object.command.*;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.interaction.*;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.*;
import reactor.core.publisher.Mono;

@ChatInputCommand(name = "r", description = "Change text layout.")
public class TextLayoutCommand extends BaseInteractionCommand{

    public TextLayoutCommand(){

        addOption(builder -> builder.name("type")
                .description("Text layout type.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
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
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true));
    }

    @Override
    public Mono<Void> execute(CommandEnvironment env){
        boolean russian = env.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map("ru"::equals)
                .orElse(false);

        String text = env.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> russian
                        ? inside.command.common.TextLayoutCommand.text2eng(str)
                        : inside.command.common.TextLayoutCommand.text2rus(str))
                .orElseGet(() -> messageService.get(env.context(), "message.placeholder"));

        return env.event().reply(text);
    }
}
