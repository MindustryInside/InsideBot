package inside.util;

import java.util.function.Supplier;

public abstract class Preconditions{

    private Preconditions(){}

    public static void requireArgument(boolean expression){
        if(!expression){
            throw new IllegalArgumentException();
        }
    }

    public static void requireArgument(boolean expression, String message){
        if(!expression){
            throw new IllegalArgumentException(message);
        }
    }

    public static void requireArgument(boolean expression, Supplier<String> message){
        if(!expression){
            throw new IllegalArgumentException(message.get());
        }
    }

    public static void requireState(boolean expression){
        if(!expression){
            throw new IllegalStateException();
        }
    }

    public static void requireState(boolean expression, String message){
        if(!expression){
            throw new IllegalStateException(message);
        }
    }

    public static void requireState(boolean expression, Supplier<String> message){
        if(!expression){
            throw new IllegalStateException(message.get());
        }
    }
}
