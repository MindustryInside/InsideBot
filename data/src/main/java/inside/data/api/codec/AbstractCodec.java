package inside.data.api.codec;

import io.netty.buffer.ByteBuf;
import io.r2dbc.postgresql.client.EncodedParameter;
import io.r2dbc.postgresql.codec.Codec;
import io.r2dbc.postgresql.codec.CodecMetadata;
import io.r2dbc.postgresql.codec.PostgresTypeIdentifier;
import io.r2dbc.postgresql.codec.PostgresqlObjectId;
import io.r2dbc.postgresql.message.Format;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

import static io.r2dbc.postgresql.client.EncodedParameter.NULL_VALUE;

public abstract class AbstractCodec<T> implements Codec<T>, CodecMetadata {

    private final Class<T> type;

    protected AbstractCodec(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public boolean canDecode(int dataType, Format format, Class<?> type) {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(type, "type");

        return (type == Object.class || type.isAssignableFrom(this.type)) && doCanDecode(getDataType(dataType), format);
    }

    protected static EncodedParameter create(Format format, PostgresTypeIdentifier type, Publisher<? extends ByteBuf> value) {
        Objects.requireNonNull(type, "type");
        return new EncodedParameter(format, type.getObjectId(), value);
    }

    protected static EncodedParameter create(Format format, PostgresTypeIdentifier type, Supplier<? extends ByteBuf> bufferSupplier) {
        Objects.requireNonNull(type, "type");
        return create(format, type.getObjectId(), bufferSupplier);
    }

    protected static EncodedParameter create(Format format, int type, Supplier<? extends ByteBuf> bufferSupplier) {
        return new EncodedParameter(format, type, Mono.fromSupplier(bufferSupplier));
    }

    protected static EncodedParameter createNull(Format format, PostgresTypeIdentifier type) {
        return create(format, type, NULL_VALUE);
    }

    @Override
    public boolean canEncode(Object value) {
        Objects.requireNonNull(value, "value");

        return type.isInstance(value);
    }

    @Override
    public boolean canEncodeNull(Class<?> type) {
        Objects.requireNonNull(type, "type");

        return this.type.isAssignableFrom(type);
    }

    @Nullable
    @Override
    public final T decode(@Nullable ByteBuf buffer, int dataType, Format format, Class<? extends T> type) {
        if (buffer == null) {
            return null;
        }

        return doDecode(buffer, getDataType(dataType), format, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final EncodedParameter encode(Object value) {
        Objects.requireNonNull(value, "value");

        return doEncode((T) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EncodedParameter encode(Object value, int dataType) {
        Objects.requireNonNull(value, "value");

        return doEncode((T) value, getDataType(dataType));
    }

    protected PostgresTypeIdentifier getDataType(int dataType) {
        return PostgresqlObjectId.isValid(dataType) ? PostgresqlObjectId.valueOf(dataType) : () -> dataType;
    }

    @Override
    public Class<?> type() {
        return type;
    }

    protected abstract boolean doCanDecode(PostgresTypeIdentifier type, Format format);

    protected abstract T doDecode(ByteBuf buffer, PostgresTypeIdentifier dataType, Format format, Class<? extends T> type);

    protected abstract EncodedParameter doEncode(T value);

    protected abstract EncodedParameter doEncode(T value, PostgresTypeIdentifier dataType);
}
