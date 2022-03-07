package inside.util.expression;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.function.BiFunction;

public interface Function {

    static Function createFixed(String name, int parametersCount, BiFunction<MathContext, List<BigDecimal>, BigDecimal> func) {
        return createFixed(name, parametersCount, parametersCount, func);
    }

    static Function createFixed(String name, int minParametersCount, int maxParametersCount,
                                BiFunction<MathContext, List<BigDecimal>, BigDecimal> func) {
        return create(name, minParametersCount, maxParametersCount, func);
    }

    static Function create(String name, int minParametersCount, int maxParametersCount,
                           BiFunction<MathContext, List<BigDecimal>, BigDecimal> func) {
        return new BaseFunction(name, minParametersCount, maxParametersCount) {
            @Override
            public BigDecimal eval(MathContext mc, List<BigDecimal> params) {
                return func.apply(mc, params);
            }
        };
    }

    String name();

    int minParametersCount();

    int maxParametersCount();

    BigDecimal eval(MathContext mc, List<BigDecimal> params);
}
