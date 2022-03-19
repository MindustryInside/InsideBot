package inside.data.schedule;

import inside.util.Preconditions;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

public abstract class AbstractTrigger implements Trigger {
    protected final Key key;
    protected final int priority;
    protected final Instant startTimestamp;
    protected final int misfireInstruction;
    @Nullable
    protected final Instant endTimestamp;
    @Nullable
    protected Key jobKey;
    @Nullable
    protected String fireInstanceId;

    @Nullable
    protected Instant nextFireTimestamp;
    @Nullable
    protected Instant prevFireTimestamp;

    protected AbstractTrigger(Key key, int priority, Instant startTimestamp,
                              int misfireInstruction, @Nullable Instant endTimestamp) {
        this.key = key;
        this.priority = priority;
        this.startTimestamp = startTimestamp;
        this.misfireInstruction = misfireInstruction;
        this.endTimestamp = endTimestamp;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public Key jobKey() {
        Preconditions.requireState(jobKey != null);
        return jobKey;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Instant startTimestamp() {
        return startTimestamp;
    }

    @Nullable
    @Override
    public Instant endTimestamp() {
        return endTimestamp;
    }

    @Nullable
    @Override
    public Instant nextFireTimestamp() {
        return nextFireTimestamp;
    }

    @Nullable
    @Override
    public Instant previousFireTimestamp() {
        return prevFireTimestamp;
    }

    @Override
    public void setJobKey(Key jobKey) {
        Preconditions.requireState(this.jobKey == null);
        this.jobKey = Objects.requireNonNull(jobKey, "jobKey");
    }

    @Override
    public String getFireInstanceId() {
        return fireInstanceId;
    }

    @Override
    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = Objects.requireNonNull(fireInstanceId, "fireInstanceId");
    }

    @Override
    public int misfireInstruction() {
        return misfireInstruction;
    }

    @Override
    public String toString() {
        return "AbstractTrigger{" +
                "key=" + key +
                ", jobKey=" + jobKey +
                ", priority=" + priority +
                ", startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", nextFireTimestamp=" + nextFireTimestamp +
                ", prevFireTimestamp=" + prevFireTimestamp +
                '}';
    }
}
