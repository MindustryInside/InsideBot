package inside.util;

import reactor.util.context.Context;

import java.util.function.UnaryOperator;

public abstract class ContextUtil{
    public static final String KEY_GUILD_ID = "inside.guild";
    public static final String KEY_LOCALE = "inside.locale";
    public static final String KEY_TIMEZONE = "inside.timezone";

    private ContextUtil(){}

    public static UnaryOperator<Context> reset(){
        return ctx -> ctx.delete(KEY_LOCALE)
                .delete(KEY_TIMEZONE)
                .delete(KEY_GUILD_ID);
    }
}
