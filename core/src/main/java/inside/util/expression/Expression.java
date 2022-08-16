package inside.util.expression;

import reactor.util.annotation.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class Expression {
    public static final String MISSING_PARAMETERS_FOR_OPERATOR = "Missing parameter(s) for operator ";
    protected static final BigDecimal PARAMS_START = BigDecimal.valueOf(-1);

    protected static Map<String, Operator> defaultOperators;
    protected static Map<String, Function> defaultFunctions;
    protected static Map<String, BigDecimal> defaultVariables;

    protected static Map<String, Operator> getDefaultOperators() {
        if (defaultOperators != null) { // weeeee, гонки
            return defaultOperators;
        }

        defaultOperators = new HashMap<>();
        addOperator(defaultOperators, Operator.create("+", true, 10, (mc, a, b) -> a.add(b, mc)));
        addOperator(defaultOperators, Operator.create("-", true, 10, (mc, a, b) -> a.subtract(b, mc)));
        addOperator(defaultOperators, Operator.create("*", true, 20, (mc, a, b) -> a.multiply(b, mc)));
        addOperator(defaultOperators, Operator.create("/", true, 20, (mc, a, b) -> a.divide(b, mc)));
        addAlias(defaultOperators, "/", ":");
        addOperator(defaultOperators, Operator.create("%", true, 20, (mc, a, b) -> a.remainder(b, mc)));

        addOperator(defaultOperators, Operator.createUnary("+", false, 10, (mc, a) -> a));
        addOperator(defaultOperators, Operator.createUnary("-", false, 10, (mc, a) -> a.negate(mc)));

        return defaultOperators;
    }

    protected static Map<String, Function> getDefaultFunctions() {
        if (defaultFunctions != null) {
            return defaultFunctions;
        }

        defaultFunctions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        addFunction(defaultFunctions, Function.create("max", 1, Integer.MAX_VALUE,
                (mc, params) -> params.stream()
                        .reduce(BigDecimal::max)
                        .orElseThrow()));
        addFunction(defaultFunctions, Function.create("min", 1, Integer.MAX_VALUE,
                (mc, params) -> params.stream()
                        .reduce(BigDecimal::min)
                        .orElseThrow()));
        addFunction(defaultFunctions, Function.createFixed("clamp", 3, (mc, params) -> {
            BigDecimal value = params.get(0);
            BigDecimal min = params.get(1);
            BigDecimal max = params.get(2);

            return min.max(max.min(value));
        }));
        addFunction(defaultFunctions, Function.createFixed("abc", 1, (mc, params) -> {
            BigDecimal value = params.get(0);
            return value.abs(mc);
        }));
        addFunction(defaultFunctions, Function.createFixed("ceil", 1, (mc, params) -> {
            BigDecimal value = params.get(0);
            return value.setScale(0, RoundingMode.CEILING);
        }));
        addFunction(defaultFunctions, Function.createFixed("round", 1, (mc, params) -> {
            BigDecimal value = params.get(0);
            return value.setScale(0, mc.getRoundingMode());
        }));
        addFunction(defaultFunctions, Function.createFixed("floor", 1, (mc, params) -> {
            BigDecimal value = params.get(0);
            return value.setScale(0, RoundingMode.FLOOR);
        }));
        addFunction(defaultFunctions, Function.createFixed("millis", 0, (mc, params) ->
                BigDecimal.valueOf(System.currentTimeMillis())));
        addFunction(defaultFunctions, Function.createFixed("radians", 1, (mc, params) ->
                BigDecimal.valueOf(Math.toRadians(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("decrees", 1, (mc, params) ->
                BigDecimal.valueOf(Math.toDegrees(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("decrees", 1, (mc, params) ->
                BigDecimal.valueOf(Math.toDegrees(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("sin", 1, (mc, params) ->
                BigDecimal.valueOf(Math.sin(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("sinh", 1, (mc, params) ->
                BigDecimal.valueOf(Math.sinh(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("asin", 1, (mc, params) ->
                BigDecimal.valueOf(Math.asin(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("cos", 1, (mc, params) ->
                BigDecimal.valueOf(Math.cos(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("cosh", 1, (mc, params) ->
                BigDecimal.valueOf(Math.cosh(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("acos", 1, (mc, params) ->
                BigDecimal.valueOf(Math.acos(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("tan", 1, (mc, params) ->
                BigDecimal.valueOf(Math.tan(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("tanh", 1, (mc, params) ->
                BigDecimal.valueOf(Math.tanh(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("atan", 1, (mc, params) ->
                BigDecimal.valueOf(Math.atan(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("cbrt", 1, (mc, params) ->
                BigDecimal.valueOf(Math.cbrt(params.get(0).doubleValue()))));
        addFunction(defaultFunctions, Function.createFixed("sqrt", 1, (mc, params) -> params.get(0).sqrt(mc)));

        return defaultFunctions;
    }

    protected static Map<String, BigDecimal> getDefaultVariables() {
        if (defaultVariables != null) {
            return defaultVariables;
        }

        defaultVariables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        defaultVariables.put("pi", BigDecimal.valueOf(Math.PI));
        defaultVariables.put("e", BigDecimal.valueOf(Math.E));

        return defaultVariables;
    }

    private static void addFunction(Map<String, Function> map, Function fn) {
        map.put(fn.name(), fn);
    }

    private static <T> void addAlias(Map<String, T> map, String name, String... variants) {
        T op = map.get(name);
        for (String variant : variants) {
            map.put(variant, op);
        }
    }

    private static void addOperator(Map<String, Operator> map, Operator op) {
        String name = op.name();
        if (op.isUnaryOperator()) {
            name += 'u';
        }

        map.put(name, op);
    }

    protected Map<String, Operator> operators = new HashMap<>();
    protected Map<String, Function> functions = new HashMap<>();
    protected Map<String, BigDecimal> variables = new HashMap<>();

    protected final String str;
    protected final MathContext mc;

    public Expression(String str) {
        this(str, MathContext.DECIMAL64);
    }

    public Expression(String str, MathContext mc) {
        this.str = Objects.requireNonNull(str);
        this.mc = Objects.requireNonNull(mc);
    }

    protected Map<String, Operator> getOperators() {
        if (operators.isEmpty()) {
            operators.putAll(getDefaultOperators());
        }
        return operators;
    }

    protected Map<String, Function> getFunctions() {
        if (functions.isEmpty()) {
            functions.putAll(getDefaultFunctions());
        }
        return functions;
    }

    protected Map<String, BigDecimal> getVariables() {
        if (variables.isEmpty()) {
            variables.putAll(getDefaultVariables());
        }
        return variables;
    }

    public Expression addVariable(String name, BigDecimal var) {
        getVariables().put(name, var);
        return this;
    }

    public Expression addOperator(Operator op) {;
        addOperator(getOperators(), op);
        return this;
    }

    public Expression addAliasOperator(String original, String... vars) {
        addAlias(getOperators(), original, vars);
        return this;
    }

    public Expression addAliasFunction(String original, String... vars) {
        addAlias(getFunctions(), original, vars);
        return this;
    }

    public Expression addFunction(Function fn) {
        addFunction(getFunctions(), fn);
        return this;
    }

    public BigDecimal eval() {
        var tkns = shuntingYard(str);
        validate(tkns);

        LinkedList<BigDecimal> stack = new LinkedList<>();
        for (Token token : tkns) {
            switch (token.type) {
                case UNARY_OPERATOR -> {
                    BigDecimal value = stack.pop();
                    BigDecimal result = getOperators().get(token.str).eval(mc, value, null);
                    stack.push(result);
                }
                case OPERATOR -> {
                    BigDecimal p1 = stack.pop();
                    BigDecimal p2 = stack.pop();
                    BigDecimal result = getOperators().get(token.str).eval(mc, p2, p1);
                    stack.push(result);
                }
                case VARIABLE -> {
                    if (!getVariables().containsKey(token.str)) {
                        throw new ExpressionException("Unknown operator or function: " + token);
                    }

                    stack.push(getVariables().get(token.str));
                }
                case FUNCTION -> {
                    Function func = getFunctions().get(token.str);
                    List<BigDecimal> params = new ArrayList<>(func.minParametersCount());

                    while (!stack.isEmpty() && !Objects.equals(stack.peek(), PARAMS_START)) {
                        params.add(0, stack.pop());
                    }

                    if (Objects.equals(stack.peek(), PARAMS_START)) {
                        stack.pop();
                    }

                    BigDecimal result = func.eval(mc, params);
                    stack.push(result);
                }
                case OPEN_PARENTHESIS -> stack.push(PARAMS_START);
                case LITERAL -> {
                    BigDecimal val = new BigDecimal(token.str, mc);
                    stack.push(val);
                }
                default -> throw new ExpressionException("Unexpected token " + token.str, token.pos);
            }
        }

        return stack.pop();
    }

    private List<Token> shuntingYard(String expression) {
        List<Token> out = new ArrayList<>();
        LinkedList<Token> stack = new LinkedList<>();

        Tokenizer tkn = new Tokenizer(expression);

        Token lastFunction = null;
        Token t;
        Token prev = null;
        while ((t = tkn.nextToken()) != null) {
            switch (t.type) {
                case LITERAL -> {
                    if (prev != null && prev.type == Token.Type.LITERAL) {
                        throw new ExpressionException("Missing operator", t.pos);
                    }
                    out.add(t);
                }
                case VARIABLE -> out.add(t);
                case FUNCTION -> {
                    lastFunction = t;
                    stack.addFirst(t);
                }
                case COMMA -> {
                    if (prev != null && prev.type == Token.Type.OPERATOR) {
                        throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + prev.str, prev.pos);
                    }
                    while (!stack.isEmpty() && stack.getFirst().type != Token.Type.OPEN_PARENTHESIS) {
                        out.add(stack.removeFirst());
                    }
                    if (stack.isEmpty()) {
                        throw new ExpressionException(lastFunction == null ? "Unexpected comma" :
                                "Parse error for function " + lastFunction.str, t.pos);
                    }
                }
                case OPERATOR -> {
                    if (prev != null && getOperators().containsKey(t.str)
                            && (prev.type == Token.Type.COMMA
                            || prev.type == Token.Type.OPEN_PARENTHESIS)) {
                        if (!getOperators().get(t.str).isUnaryOperator()) {
                            throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + t, t.pos);
                        }
                    }

                    var op = getOperators().get(t.str);
                    if (op == null) {
                        throw new ExpressionException("Unknown operator " + t.str, t.pos + 1);
                    }

                    shuntOperators(out, stack, op);
                    stack.addFirst(t);
                }
                case UNARY_OPERATOR -> {
                    if (prev != null && prev.type != Token.Type.OPERATOR
                            && prev.type != Token.Type.COMMA && prev.type != Token.Type.OPEN_PARENTHESIS
                            && prev.type != Token.Type.UNARY_OPERATOR) {
                        throw new ExpressionException("Invalid position for unary operator " + t.str, t.pos);
                    }

                    var op = getOperators().get(t.str);
                    if (op == null) {
                        throw new ExpressionException("Unknown unary operator "
                                + t.str.substring(0, t.str.length() - 1), t.pos + 1);
                    }

                    shuntOperators(out, stack, op);
                    stack.addFirst(t);
                }
                case OPEN_PARENTHESIS -> {
                    if (prev != null) {
                        if (prev.type == Token.Type.LITERAL
                                || prev.type == Token.Type.CLOSE_PARENTHESIS
                                || prev.type == Token.Type.VARIABLE) {
                            Token mult = new Token(-1, "*", Token.Type.OPERATOR);
                            stack.addFirst(mult);
                        }

                        if (prev.type == Token.Type.FUNCTION) {
                            out.add(t);
                        }
                    }
                    stack.addFirst(t);
                }
                case CLOSE_PARENTHESIS -> {
                    if (prev != null && prev.type == Token.Type.OPERATOR &&
                            !operators.get(prev.str).isUnaryOperator()) {
                        throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + prev.str, prev.pos);
                    }
                    while (!stack.isEmpty() && stack.getFirst().type != Token.Type.OPEN_PARENTHESIS) {
                        out.add(stack.removeFirst());
                    }
                    if (stack.isEmpty()) {
                        throw new ExpressionException("Mismatched parentheses");
                    }
                    stack.removeFirst();
                    if (!stack.isEmpty() && stack.getFirst().type == Token.Type.FUNCTION) {
                        out.add(stack.removeFirst());
                    }
                }
            }

            prev = t;
        }

        while (!stack.isEmpty()) {
            Token element = stack.pop();
            if (element.type == Token.Type.OPEN_PARENTHESIS || element.type == Token.Type.CLOSE_PARENTHESIS) {
                throw new ExpressionException("Mismatched parentheses");
            }
            out.add(element);
        }
        return out;
    }

    private void validate(List<Token> rpn) {
        LinkedList<Integer> list = new LinkedList<>();
        list.addFirst(0);

        for (Token token : rpn) {
            switch (token.type) {
                case UNARY_OPERATOR -> {
                    if (list.getFirst() < 1) {
                        throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + token.str);
                    }
                }
                case OPERATOR -> {
                    var op = getOperators().get(token.str);
                    int ops = op.isUnaryOperator() ? 1 : 2;
                    if (list.getFirst() < ops) {
                        throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + token.str);
                    }

                    if (ops > 1) {
                        list.addLast(list.removeFirst() - ops + 1);
                    }
                }
                case FUNCTION -> {
                    Function func = getFunctions().get(token.str);
                    if (func == null) {
                        throw new ExpressionException("Unknown function " + token.str, token.pos + 1);
                    }

                    int numParams = list.removeFirst();
                    if (func.minParametersCount() != func.maxParametersCount() &&
                            (numParams < func.minParametersCount() || numParams > func.maxParametersCount())) {
                        throw new ExpressionException("Function " + token.str + " expected " + func.minParametersCount() + "-"
                                + func.maxParametersCount() + " parameters, got " + numParams);
                    }

                    if (list.isEmpty()) {
                        throw new ExpressionException("Too many function calls, maximum scope exceeded");
                    }

                    list.addLast(list.removeFirst() + 1);
                }
                case OPEN_PARENTHESIS -> list.addFirst(0);
                default -> list.addLast(list.removeFirst() + 1);
            }
        }

        if (list.size() > 1) {
            throw new ExpressionException("Too many unhandled function parameter lists");
        }
        if (list.getFirst() > 1) {
            throw new ExpressionException("Too many numbers or variables");
        }
        if (list.getFirst() < 1) {
            throw new ExpressionException("Empty expression");
        }
    }

    private void shuntOperators(List<Token> outputQueue, LinkedList<Token> stack, Operator op) {
        Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null && (nextToken.type == Token.Type.OPERATOR || nextToken.type == Token.Type.UNARY_OPERATOR)
                && (op.isLeftAssociative() && op.priority() <= operators.get(nextToken.str).priority()
                || op.priority() < operators.get(nextToken.str).priority())) {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }

    public static class ExpressionException extends RuntimeException {

        public ExpressionException(String message) {
            super(message);
        }

        public ExpressionException(String message, int characterPosition) {
            super(message + " at character position " + characterPosition);
        }
    }

    protected record Token(int pos, String str, Expression.Token.Type type) {

        public enum Type {
            CLOSE_PARENTHESIS,
            COMMA,
            LITERAL,
            OPEN_PARENTHESIS,
            OPERATOR,
            FUNCTION, VARIABLE, UNARY_OPERATOR
        }
    }

    protected class Tokenizer {

        protected final String str;

        int cursor = 0;
        Token prev = null;

        public Tokenizer(String str) {
            this.str = str.trim();
        }

        @Nullable
        public Token nextToken() {
            if (cursor >= str.length()) {
                return null;
            }

            char c = str.charAt(cursor);
            while (Character.isWhitespace(c) && cursor < str.length()) {
                c = str.charAt(++cursor);
            }

            int pos = cursor;
            Token.Type type;

            StringBuilder tkn = new StringBuilder();
            if (Character.isDigit(c) || c == '.' && cursor + 1 < str.length() &&
                    Character.isDigit(str.charAt(cursor + 1))) {

                while ((Character.isDigit(c) || c == '.' || Character.toLowerCase(c) == 'e' ||
                        (c == '+' || c == '-') && tkn.length() > 0 &&
                        Character.toLowerCase(tkn.charAt(tkn.length() - 1)) == 'e')
                        && cursor < str.length()) {
                    tkn.append(str.charAt(cursor++));
                    c = cursor >= str.length() ? '\0' : str.charAt(cursor);
                }

                type = Token.Type.LITERAL;
            } else if (Character.isLetter(c)) {
                while ((Character.isLetter(c) || Character.isDigit(c) || tkn.length() == 0) && cursor < str.length()) {
                    tkn.append(str.charAt(cursor++));
                    c = cursor == str.length() ? '\0' : str.charAt(cursor);
                }

                if (Character.isWhitespace(c)) {
                    while (Character.isWhitespace(c) && cursor < str.length()) {
                        c = str.charAt(cursor++);
                    }
                    cursor--;
                }

                if (getOperators().containsKey(tkn.toString())) {
                    type = Token.Type.OPERATOR;
                } else if (c == '(') {
                    type = Token.Type.FUNCTION;
                } else {
                    type = Token.Type.VARIABLE;
                }
            } else if (c == '(' || c == ')' || c == ',') {
                type = switch (c) {
                    case '(' -> Token.Type.OPEN_PARENTHESIS;
                    case ')' -> Token.Type.CLOSE_PARENTHESIS;
                    default -> Token.Type.COMMA;
                };

                tkn.append(c);
                cursor++;
            } else {
                StringBuilder subs = new StringBuilder();
                int end = -1;
                while (!Character.isLetter(c) && !Character.isDigit(c) && !Character.isWhitespace(c) && cursor < str.length()) {
                    cursor++;
                    subs.append(c);
                    if (getOperators().containsKey(subs.toString())) {
                        end = cursor;
                    }

                    c = cursor >= str.length() ? '\0' : str.charAt(cursor);
                }
                if (end != -1) {
                    tkn.append(str, pos, end);
                    cursor = end;
                } else {
                    tkn.append(subs);
                }

                if (prev == null || prev.type == Token.Type.OPERATOR && !getOperators().get(prev.str).isUnaryOperator()
                        || prev.type == Token.Type.OPEN_PARENTHESIS || prev.type == Token.Type.COMMA
                        || prev.type == Token.Type.UNARY_OPERATOR) {
                    tkn.append('u');
                    type = Token.Type.UNARY_OPERATOR;
                } else {
                    type = Token.Type.OPERATOR;
                }
            }

            Token token = new Token(pos, tkn.toString(), type);

            prev = token;
            return token;
        }
    }
}
