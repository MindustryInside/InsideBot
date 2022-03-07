package inside.util.expression;

import java.math.BigDecimal;
import java.math.MathContext;

public abstract class BaseUnaryOperator extends BaseOperator {

    public BaseUnaryOperator(String name, boolean leftAssociative, int priority) {
        super(name, leftAssociative, true, priority);
    }

    @Override
    public final boolean isUnaryOperator() {
        return true;
    }

    @Override
    public final BigDecimal eval(MathContext mc, BigDecimal a, BigDecimal b) {
        return eval(mc, a);
    }

    public abstract BigDecimal eval(MathContext mc, BigDecimal a);
}
