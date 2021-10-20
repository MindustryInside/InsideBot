package inside.command.common;

import com.udojava.evalex.*;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.util.Strings;
import inside.util.io.ReusableByteInputStream;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.*;
import java.util.List;
import java.util.function.Supplier;

import static inside.audit.BaseAuditProvider.MESSAGE_TXT;

@DiscordCommand(key = {"math", "calc"}, params = "command.math.params", description = "command.math.description")
public class MathCommand extends Command{

    private static final LazyOperator divideAlias = new AbstractOperator(":", Expression.OPERATOR_PRECEDENCE_MULTIPLICATIVE, true){
        @Override
        public BigDecimal eval(BigDecimal v1, BigDecimal v2){
            return v1.divide(v2, MathContext.DECIMAL32);
        }
    };

    private static final LazyFunction factorialFunction = new AbstractLazyFunction("FACT", 1){
        @Override
        public Expression.LazyNumber lazyEval(List<Expression.LazyNumber> lazyParams){
            var fist = lazyParams.get(0);
            if(fist.eval().longValue() > 100){
                throw new ArithmeticException("The number is too big!");
            }

            return createNumber(() -> {
                int number = lazyParams.get(0).eval().intValue();
                BigDecimal factorial = BigDecimal.ONE;
                for(int i = 1; i <= number; i++){
                    factorial = factorial.multiply(new BigDecimal(i));
                }
                return factorial;
            });
        }
    };

    private static final LazyFunction levenshteinDstFunction = new AbstractLazyFunction("LEVEN", 2){
        @Override
        public Expression.LazyNumber lazyEval(List<Expression.LazyNumber> lazyParams){
            var first = lazyParams.get(0);
            var second = lazyParams.get(1);
            return createNumber(() -> BigDecimal.valueOf(Strings.damerauLevenshtein(first.getString(), second.getString())));
        }
    };

    public static Mono<BigDecimal> createExpression(String text){
        return Mono.fromCallable(() -> {
            Expression exp = new Expression(text);
            exp.addOperator(divideAlias);
            exp.addLazyFunction(levenshteinDstFunction);
            exp.addLazyFunction(factorialFunction);
            return exp.eval();
        });
    }

    private static Expression.LazyNumber createNumber(Supplier<BigDecimal> bigDecimal){
        return new Expression.LazyNumber(){
            @Override
            public BigDecimal eval(){
                return bigDecimal.get();
            }

            @Override
            public String getString(){
                return eval().toPlainString();
            }
        };
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        return createExpression(text).publishOn(Schedulers.boundedElastic())
                .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException ||
                                t instanceof NumberFormatException,
                        t -> messageService.errTitled(env, "command.math.error.title", t.getMessage()).then(Mono.empty()))
                .map(BigDecimal::toString)
                .flatMap(decimal -> {
                    var messageSpec = MessageCreateSpec.builder()
                            .messageReference(env.message().getId());

                    if(decimal.isBlank()){
                        messageSpec.content(messageService.get(env.context(), "message.placeholder"));
                    }else if(decimal.length() >= Message.MAX_CONTENT_LENGTH){
                        messageSpec.addFile(MESSAGE_TXT, ReusableByteInputStream.ofString(decimal));
                    }else{
                        messageSpec.content(decimal);
                    }

                    return env.channel().createMessage(messageSpec.build())
                            .then();
                });
    }

    @Override
    public Publisher<?> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.math.help",
                GuildConfig.formatPrefix(prefix))
                .then();
    }
}
