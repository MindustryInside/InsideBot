package inside.util;

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
}
