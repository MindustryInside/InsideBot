package inside.util.expression;

import reactor.function.Function3;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.BiFunction;

public interface Operator {

    static Operator createUnary(String name, boolean leftAssoc, int priority,
                                BiFunction<MathContext, BigDecimal, BigDecimal> func) {
        return new BaseUnaryOperator(name, leftAssoc, priority) {
            @Override
            public BigDecimal eval(MathContext mc, BigDecimal a) {
                return func.apply(mc, a);
            }
        };
    }

    static Operator create(String name, boolean leftAssoc, int priority,
                           Function3<MathContext, BigDecimal, BigDecimal, BigDecimal> func) {
        return new BaseOperator(name, leftAssoc, false, priority) {
            @Override
            public BigDecimal eval(MathContext mc, BigDecimal a, BigDecimal b) {
                return func.apply(mc, a, b);
            }
        };
    }

    String name();

    boolean isLeftAssociative();

    boolean isUnaryOperator();

    int priority();

    BigDecimal eval(MathContext mc, BigDecimal a, BigDecimal b);
}
