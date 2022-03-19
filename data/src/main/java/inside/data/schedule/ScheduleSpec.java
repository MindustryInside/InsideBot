package inside.data.schedule;

import reactor.util.annotation.Nullable;

import java.time.Instant;

public abstract class ScheduleSpec<T extends Trigger> {

    public abstract Key key();

    public Instant startTimestamp(){
        return Instant.now();
    }

    @Nullable
    public abstract Instant endTimestamp();

    public int priority(){
        return Trigger.DEFAULT_PRIORITY;
    }

    public int misfireInstruction() {
        return Trigger.MISFIRE_SMART_POLICY;
    }

    public abstract T asTrigger();
}
