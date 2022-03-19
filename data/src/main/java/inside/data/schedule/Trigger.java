package inside.data.schedule;

import reactor.util.annotation.Nullable;

import java.time.Instant;

public interface Trigger {

    int MISFIRE_SMART_POLICY = 0;
    int MISFIRE_IGNORE_POLICY = -1;

    int DEFAULT_PRIORITY = 5;

    Key key();

    Key jobKey();

    int priority();

    Instant startTimestamp();

    int misfireInstruction();

    @Nullable
    Instant endTimestamp();

    @Nullable
    Instant nextFireTimestamp();

    @Nullable
    Instant previousFireTimestamp();

    @Nullable
    Instant getFireTimestampAfter(Instant afterTimestamp);

    void setJobKey(Key jobKey);

    void triggered();

    Instant computeFirstFireTimestamp();

    void updateAfterMisfire();

    @Nullable
    String getFireInstanceId();

    void setFireInstanceId(String fireInstanceId);

    void setNextFireTimestamp(@Nullable Instant nextFireTimestamp);

    void setPreviousFireTimestamp(@Nullable Instant previousFireTimestamp);

    ScheduleSpec<? extends Trigger> scheduleSpec();

    enum State {
        waiting, complete, blocked, acquired, executing
    }
}
