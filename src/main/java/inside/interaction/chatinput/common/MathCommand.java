package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.Message;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import inside.util.MessageUtil;
import inside.util.expression.Expression;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ChatInputCommand(value = "commands.common.math")
public class MathCommand extends InteractionCommand {

    public MathCommand(MessageService messageService) {
        super(messageService);

        addOption("expression", spec -> spec.type(Type.STRING.getValue()).required(true));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        String expression = env.getOption("expression")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow();

        return Mono.fromCallable(() -> new Expression(expression).eval())
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException ||
                                t instanceof NumberFormatException,
                        t -> messageService.err(env, "commands.common.math.invalid-expression").then(Mono.empty()))
                .flatMap(decimal -> env.event().deferReply()
                        .then(env.event().editReply(MessageUtil.substringTo(expression + " = " + decimal.toString(),
                                Message.MAX_CONTENT_LENGTH))));
    }
}
