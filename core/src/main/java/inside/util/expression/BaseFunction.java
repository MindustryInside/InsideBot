package inside.util.expression;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;

public abstract class BaseFunction implements Function {
    protected final String name;
    protected final int maxParametersCount;
    protected final int minParametersCount;

    public BaseFunction(String name, int minParametersCount, int maxParametersCount) {
        this.name = Objects.requireNonNull(name, "name");
        this.minParametersCount = minParametersCount;
        this.maxParametersCount = maxParametersCount;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int minParametersCount() {
        return minParametersCount;
    }

    @Override
    public int maxParametersCount() {
        return maxParametersCount;
    }

    @Override
    public abstract BigDecimal eval(MathContext mc, List<BigDecimal> params);
}
