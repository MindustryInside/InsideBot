package inside.util;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    private static final Lazy<?> EMPTY = new Lazy<>(() -> null, true, null);

    private final Supplier<? extends T> delegate;
    private final AtomicBoolean initialized = new AtomicBoolean();
    @Nullable
    private T value;

    public Lazy(Supplier<? extends T> delegate, boolean initialized, @Nullable T value) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.initialized.set(initialized);
        this.value = value;
    }

    private Lazy(Supplier<? extends T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public Supplier<? extends T> getDelegate() {
        return delegate;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    @Nullable
    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> Lazy<T> empty() {
        return (Lazy<T>) EMPTY;
    }

    public static <T> Lazy<T> of(T value) {
        if (value == null) {
            return empty();
        }
        return new Lazy<>(() -> value, true, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Lazy<T> of(Supplier<? extends T> delegate) {
        if (delegate.equals(EMPTY)) {
            return empty();
        }
        if (delegate instanceof Lazy) {
            return (Lazy<T>) delegate;
        }
        return new Lazy<>(delegate);
    }

    @Override
    public T get() {
        if (initialized.compareAndSet(false, true)) {
            value = delegate.get();
            initialized.set(true);
        }

        return value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lazy<?> lazy = (Lazy<?>) o;
        return delegate.equals(lazy.delegate) && initialized.equals(lazy.initialized) && Objects.equals(value, lazy.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, initialized, value);
    }

    @Override
    public String toString() {
        return "Lazy{" + value + '}';
    }
}
