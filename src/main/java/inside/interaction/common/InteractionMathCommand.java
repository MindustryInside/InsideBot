package inside.interaction.common;

import com.udojava.evalex.Expression;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import inside.util.MessageUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@InteractionDiscordCommand(name = "math", description = "Calculate math expression.")
public class InteractionMathCommand extends BaseInteractionCommand{

    public InteractionMathCommand(){

        addOption(builder -> builder.name("expression")
                .description("Math expression.")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        String expression = env.getOption("expression")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        return inside.command.common.MathCommand.createExpression(expression).publishOn(Schedulers.boundedElastic())
                .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException ||
                                t instanceof NumberFormatException,
                        t -> messageService.errTitled(env.event(), "command.math.error.title", t.getMessage()).then(Mono.empty()))
                .flatMap(decimal -> messageService.text(env.event(), MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
    }
}
