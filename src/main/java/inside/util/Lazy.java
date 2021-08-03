package inside.util;

import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T>{
    private final Supplier<T> delegate;
    private volatile boolean initialized;
    private T value;

    private Lazy(Supplier<T> delegate){
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static <T> Lazy<T> of(Supplier<T> delegate){
        if(delegate instanceof Lazy){
            return (Lazy<T>)delegate;
        }
        return new Lazy<>(delegate);
    }

    @Override
    public T get(){
        // A 2-field variant of Double Checked Locking.
        if(!initialized){
            synchronized(this){
                if(!initialized){
                    T t = delegate.get();
                    value = t;
                    initialized = true;
                    return t;
                }
            }
        }
        return value;
    }

    @Override
    public boolean equals(Object o){
        if(this == o){
            return true;
        }
        if(o == null || getClass() != o.getClass()){
            return false;
        }
        Lazy<?> that = (Lazy<?>)o;
        return initialized == that.initialized &&
                delegate.equals(that.delegate) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode(){
        return Objects.hash(delegate, initialized, value);
    }
}
