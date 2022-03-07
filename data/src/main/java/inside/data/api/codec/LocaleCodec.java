package inside.data.api.codec;

import io.netty.buffer.ByteBufAllocator;

import java.util.Locale;

public class LocaleCodec extends ToStringCodec<Locale> {

    public LocaleCodec(ByteBufAllocator byteBufAllocator) {
        super(Locale.class, byteBufAllocator);
    }

    @Override
    protected Locale fromString(String str) {
        if (str.contains("_")) {
            String[] codes = str.split("_");
            if (codes.length == 3) {
                return new Locale(codes[0], codes[1], codes[2]);
            }
            return new Locale(codes[0], codes[1]);
        }
        return new Locale(str);
    }

    @Override
    protected String toString(Locale value) {
        return value.toString();
    }
}
