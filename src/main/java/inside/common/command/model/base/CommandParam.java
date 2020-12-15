package inside.common.command.model.base;

public class CommandParam{
    public final String name;
    public final boolean optional;
    public final boolean variadic;

    protected CommandParam(String name, boolean optional, boolean variadic){
        this.name = name;
        this.optional = optional;
        this.variadic = variadic;
    }
}
