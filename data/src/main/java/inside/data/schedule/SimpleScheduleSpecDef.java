package inside.data.schedule;

import io.r2dbc.postgresql.codec.Interval;
import org.immutables.value.Value;
import reactor.util.annotation.Nullable;

import java.time.Instant;

@SpecStyle
@Value.Immutable
abstract class SimpleScheduleSpecDef extends ScheduleSpec<SimpleTrigger> {

    @Value.Default
    public Interval repeatInterval() {
        return Interval.ZERO;
    }

    @Value.Default
    public int repeatCount() {
        return 0;
    }

    @Override
    public abstract Key key();

    @Value.Default
    @Override
    public Instant startTimestamp() {
        return super.startTimestamp();
    }

    @Nullable
    @Override
    public abstract Instant endTimestamp();

    @Value.Default
    @Override
    public int priority() {
        return super.priority();
    }

    @Value.Default
    @Override
    public int misfireInstruction() {
        return super.misfireInstruction();
    }

    @Override
    public SimpleTrigger asTrigger() {
        return new SimpleTriggerImpl(key(), priority(), startTimestamp(), misfireInstruction(),
                endTimestamp(), repeatCount(), repeatInterval());
    }

}
