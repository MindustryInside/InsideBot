package inside.command.model;

import inside.util.Strings;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class CommandOption{
    private final CommandParam param;
    @Nullable
    private final String value;

    public CommandOption(CommandParam param, @Nullable String value){
        this.param = param;
        this.value = value;
    }

    public String getName(){
        return param.name();
    }

    public boolean isOptional(){
        return param.optional();
    }

    public boolean isVariadic(){
        return param.variadic();
    }

    public Optional<OptionValue> getValue(){
        return Optional.ofNullable(value).filter(Strings::isNotEmpty).map(OptionValue::new);
    }
}
