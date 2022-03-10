package inside.util;

import java.time.Instant;

public final class InternalId {
    private InternalId() {
    }

    public static final long BOT_EPOCH = 1598361056177L;

    public static Instant getTimestamp(long id) {
        return Instant.ofEpochMilli(BOT_EPOCH + (id >>> 10));
    }

    public static int getSeqId(long id) {
        return (int) (id & 0x3ff);
    }
}
