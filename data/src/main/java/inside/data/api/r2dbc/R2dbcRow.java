package inside.data.api.r2dbc;

import inside.util.Preconditions;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import java.util.*;

public class R2dbcRow implements Row {

    private final Row delegate;

    protected R2dbcRow(Row delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static R2dbcRow of(Row row) {
        if (row instanceof R2dbcRow r) {
            return r;
        }
        return new R2dbcRow(row);
    }

    public byte getByte(int index) {
        Byte value = get(index, Byte.class);
        return value != null ? value : 0;
    }

    public byte getByte(String name) {
        Byte value = get(name, Byte.class);
        return value != null ? value : 0;
    }

    public short getShort(int index) {
        Short value = get(index, Short.class);
        return value != null ? value : 0;
    }

    public short getShort(String name) {
        Short value = get(name, Short.class);
        return value != null ? value : 0;
    }

    public char getChar(int index) {
        Character value = get(index, Character.class);
        return value != null ? value : Character.MIN_VALUE;
    }

    public char getChar(String name) {
        Character value = get(name, Character.class);
        return value != null ? value : Character.MIN_VALUE;
    }

    public boolean getBoolean(int index) {
        Boolean value = get(index, Boolean.class);
        return value != null ? value : false;
    }

    public boolean getBoolean(String name) {
        Boolean value = get(name, Boolean.class);
        return value != null ? value : false;
    }

    public int getInt(int index) {
        Integer value = get(index, Integer.class);
        return value != null ? value : 0;
    }

    public int getInt(String name) {
        Integer value = get(name, Integer.class);
        return value != null ? value : 0;
    }

    public OptionalInt getIntOptional(int index) {
        Integer value = get(index, Integer.class);
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public OptionalInt getIntOptional(String name) {
        Integer value = get(name, Integer.class);
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public long getLong(int index) {
        Long value = get(index, Long.class);
        return value != null ? value : 0;
    }

    public long getLong(String name) {
        Long value = get(name, Long.class);
        return value != null ? value : 0;
    }

    public OptionalLong getLongOptional(int index) {
        Long value = get(index, Long.class);
        return value != null ? OptionalLong.of(value) : OptionalLong.empty();
    }

    public OptionalLong getLongOptional(String name) {
        Long value = get(name, Long.class);
        return value != null ? OptionalLong.of(value) : OptionalLong.empty();
    }

    public float getFloat(int index) {
        Float value = get(index, Float.class);
        return value != null ? value : 0;
    }

    public float getFloat(String name) {
        Float value = get(name, Float.class);
        return value != null ? value : 0;
    }

    public double getDouble(int index) {
        Double value = get(index, Double.class);
        return value != null ? value : 0;
    }

    public double getDouble(String name) {
        Double value = get(name, Double.class);
        return value != null ? value : 0;
    }

    public OptionalDouble getDoubleOptional(int index) {
        Double value = get(index, Double.class);
        return value != null ? OptionalDouble.of(value) : OptionalDouble.empty();
    }

    public OptionalDouble getDoubleOptional(String name) {
        Double value = get(name, Double.class);
        return value != null ? OptionalDouble.of(value) : OptionalDouble.empty();
    }

    public <T> T getRequiredValue(int index, Class<T> type) {
        var value = get(index, type);
        Preconditions.requireState(value != null, () -> String.format("No value found for row index: %s, type: %s", index, type));
        return value;
    }

    public <T> T getRequiredValue(String name, Class<T> type) {
        var value = get(name, type);
        Preconditions.requireState(value != null, () -> String.format("No value found for row name: %s, type: %s", name, type));
        return value;
    }

    public <T> Optional<T> getOptional(int index, Class<? extends T> type) {
        return Optional.ofNullable(delegate.get(index, type));
    }

    public <T> Optional<T> getOptional(String name, Class<? extends T> type) {
        return Optional.ofNullable(delegate.get(name, type));
    }

    @Override
    public <T> T get(int index, Class<T> type) {
        return delegate.get(index, type);
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return delegate.get(name, type);
    }

    @Override
    public RowMetadata getMetadata() {
        return delegate.getMetadata();
    }
}
