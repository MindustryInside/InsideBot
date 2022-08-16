package inside.util.expression;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public abstract class BaseOperator implements Operator {

    protected final String name;
    protected final boolean leftAssociative;
    protected final boolean unary;
    protected final int priority;

    public BaseOperator(String name, boolean leftAssociative, boolean unary, int priority) {
        this.name = Objects.requireNonNull(name);
        this.leftAssociative = leftAssociative;
        this.unary = unary;
        this.priority = priority;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isLeftAssociative() {
        return leftAssociative;
    }

    @Override
    public boolean isUnaryOperator() {
        return unary;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public abstract BigDecimal eval(MathContext mc, BigDecimal a, BigDecimal b);
}
