package inside.data.api.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.client.EncodedParameter;
import io.r2dbc.postgresql.codec.PostgresTypeIdentifier;
import io.r2dbc.postgresql.message.Format;
import io.r2dbc.postgresql.util.ByteBufUtils;

import java.util.List;
import java.util.Objects;

import static io.r2dbc.postgresql.codec.PostgresqlObjectId.*;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;

public abstract class ToStringCodec<T> extends AbstractCodec<T> {

    protected final ByteBufAllocator byteBufAllocator;

    protected ToStringCodec(Class<T> type, ByteBufAllocator byteBufAllocator) {
        super(type);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator, "byteBufAllocator");
    }

    @Override
    protected boolean doCanDecode(PostgresTypeIdentifier type, Format format) {
        return type == BPCHAR || type == CHAR || type == TEXT ||
                type == UNKNOWN || type == VARCHAR || type == NAME;
    }

    @Override
    protected EncodedParameter doEncode(T value) {
        return doEncode(value, VARCHAR);
    }

    @Override
    protected EncodedParameter doEncode(T value, PostgresTypeIdentifier dataType) {
        Objects.requireNonNull(value, "value");

        return create(FORMAT_TEXT, dataType, () -> ByteBufUtils.encode(byteBufAllocator, toString(value)));
    }

    @Override
    protected T doDecode(ByteBuf buffer, PostgresTypeIdentifier dataType, Format format, Class<? extends T> type) {
        Objects.requireNonNull(buffer, "buffer");

        return fromString(ByteBufUtils.decode(buffer));
    }

    protected abstract T fromString(String str);

    protected abstract String toString(T value);

    @Override
    public Iterable<PostgresTypeIdentifier> getDataTypes() {
        return List.of(BPCHAR, CHAR, TEXT, UNKNOWN, VARCHAR, NAME);
    }

    @Override
    public EncodedParameter encodeNull() {
        return createNull(FORMAT_TEXT, VARCHAR);
    }
}
