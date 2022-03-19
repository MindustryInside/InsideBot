package inside.data.schedule;

import io.r2dbc.postgresql.codec.Interval;
import reactor.util.annotation.Nullable;

import java.math.BigInteger;
import java.time.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SimpleTriggerImpl extends AbstractTrigger implements SimpleTrigger {

    private final int repeatCount;
    private final Interval repeatInterval;

    private int timesTriggered;

    public SimpleTriggerImpl(Key key, int priority, Instant startTimestamp, int misfireInstruction,
                             @Nullable Instant endTimestamp, int repeatCount, Interval repeatInterval) {
        super(key, priority, startTimestamp, misfireInstruction, endTimestamp);
        this.repeatCount = repeatCount;
        this.repeatInterval = Objects.requireNonNull(repeatInterval, "repeatInterval");
    }

    @Override
    public int repeatCount() {
        return repeatCount;
    }

    @Override
    public Interval repeatInterval() {
        return repeatInterval;
    }

    @Override
    public int timesTriggered() {
        return timesTriggered;
    }

    public void setTimesTriggered(int timesTriggered) {
        this.timesTriggered = timesTriggered;
    }

    @Override
    public Instant getFireTimestampAfter(Instant afterTimestamp) {
        if (repeatCount != REPEAT_INDEFINITELY && timesTriggered > repeatCount) {
            return null;
        }

        if (repeatCount == 0 && afterTimestamp.compareTo(startTimestamp) >= 0) {
            return null;
        }

        if (endTimestamp != null && endTimestamp.compareTo(afterTimestamp) <= 0) {
            return null;
        }

        if (afterTimestamp.isBefore(startTimestamp)) {
            return afterTimestamp;
        }

        Duration d = Duration.between(startTimestamp, afterTimestamp);
        Period p = Period.between(LocalDate.ofInstant(startTimestamp, ZoneId.systemDefault()),
                LocalDate.ofInstant(afterTimestamp, ZoneId.systemDefault()));
        Interval between = Interval.of(p, d);

        int timesFired = repeatInterval == Interval.ZERO ? timesTriggered : toBigInteger(between)
                .divide(toBigInteger(repeatInterval)).intValueExact() + 1;
        if (timesFired > repeatCount && repeatCount != REPEAT_INDEFINITELY) {
            return null;
        }

        Instant inst = startTimestamp.plus(repeatInterval.multipliedBy(timesFired));
        if (endTimestamp != null && endTimestamp.compareTo(inst) <= 0) {
            return null;
        }

        return inst;
    }

    static BigInteger toBigInteger(Interval inter) {
        return BigInteger.valueOf(31556952L * inter.getYears())
                .add(BigInteger.valueOf(31556952L / 12 * inter.getMonths()))
                .add(BigInteger.valueOf(TimeUnit.DAYS.toSeconds(inter.getDays())))
                .add(BigInteger.valueOf(TimeUnit.HOURS.toSeconds(inter.getHours())))
                .add(BigInteger.valueOf(TimeUnit.MINUTES.toSeconds(inter.getMinutes())))
                .add(BigInteger.valueOf(inter.getSecondsInMinute()))
                .add(BigInteger.valueOf(TimeUnit.MICROSECONDS.toSeconds(inter.getMicrosecondsInSecond())));
    }

    @Override
    public void triggered() {
        timesTriggered++;
        prevFireTimestamp = nextFireTimestamp;
        nextFireTimestamp = getFireTimestampAfter(nextFireTimestamp == null
                ? Instant.now() : nextFireTimestamp);
    }

    @Override
    public Instant computeFirstFireTimestamp() {
        return nextFireTimestamp = startTimestamp;
    }

    @Override
    public void updateAfterMisfire() {
        int instr = misfireInstruction;

        if (instr == MISFIRE_SMART_POLICY) {
            instr = switch (repeatCount) {
                case 0 -> MISFIRE_FIRE_NOW;
                case REPEAT_INDEFINITELY -> MISFIRE_RESCHEDULE_REMAINING_COUNT;
                default -> MISFIRE_RESCHEDULE_REPEAT_COUNT;
            };
        }

        switch (instr) {
            case MISFIRE_FIRE_NOW -> nextFireTimestamp = Instant.now();
            case MISFIRE_RESCHEDULE_REMAINING_COUNT -> {

                Instant newFireTimestamp = getFireTimestampAfter(Instant.now());
                Instant nextFireTimestamp0 = nextFireTimestamp();
                if (newFireTimestamp != null && nextFireTimestamp0 != null) {
                    Duration d = Duration.between(newFireTimestamp, nextFireTimestamp0);
                    Period p = Period.between(LocalDate.ofInstant(newFireTimestamp, ZoneId.systemDefault()),
                            LocalDate.ofInstant(nextFireTimestamp0, ZoneId.systemDefault()));
                    Interval between = Interval.of(p, d);

                    int timesMissed = repeatInterval == Interval.ZERO ? timesTriggered : toBigInteger(between)
                            .divide(toBigInteger(repeatInterval)).intValueExact() + 1;

                    timesTriggered += timesMissed;
                }

                nextFireTimestamp = newFireTimestamp;
            }
            case MISFIRE_RESCHEDULE_REPEAT_COUNT -> {
                Instant newFireTimestamp = Instant.now();

                setNextFireTimestamp(endTimestamp != null && endTimestamp.isBefore(newFireTimestamp) ? null : newFireTimestamp);
            }
        }
    }

    @Override
    public void setNextFireTimestamp(@Nullable Instant nextFireTimestamp) {
        this.nextFireTimestamp = nextFireTimestamp;
    }

    @Override
    public void setPreviousFireTimestamp(@Nullable Instant prevFireTimestamp) {
        this.prevFireTimestamp = prevFireTimestamp;
    }

    @Override
    public ScheduleSpec<? extends Trigger> scheduleSpec() {
        return SimpleScheduleSpec.builder()
                .repeatCount(repeatCount)
                .repeatInterval(repeatInterval)
                .key(key)
                .startTimestamp(startTimestamp)
                .endTimestamp(endTimestamp)
                .priority(priority)
                .build();
    }

    @Override
    public String toString() {
        return "SimpleTriggerImpl{" +
                "repeatCount=" + repeatCount +
                ", repeatInterval=" + repeatInterval +
                ", timesTriggered=" + timesTriggered +
                "} " + super.toString();
    }
}
