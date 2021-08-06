package inside.interaction.common;

import com.udojava.evalex.Expression;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.command.Commands;
import inside.interaction.*;
import inside.util.MessageUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static inside.util.ContextUtil.KEY_EPHEMERAL;

@InteractionDiscordCommand(name = "math", description = "Calculate math expression.")
public class MathCommand extends BaseInteractionCommand{

    public MathCommand(){

        addOption(builder -> builder.name("expression")
                .description("Math expression.")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        String expression = env.event().getOption("expression")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        return Commands.MathCommand.createExpression(expression).publishOn(Schedulers.boundedElastic())
                .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException ||
                                t instanceof NumberFormatException,
                        t -> messageService.errTitled(env.event(), "command.math.error.title", t.getMessage())
                                .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true)).then(Mono.empty()))
                .flatMap(decimal -> messageService.text(env.event(), MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
    }
}
