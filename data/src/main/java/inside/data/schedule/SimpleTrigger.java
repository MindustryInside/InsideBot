package inside.data.schedule;

import io.r2dbc.postgresql.codec.Interval;

public interface SimpleTrigger extends Trigger {

    int MISFIRE_FIRE_NOW = 1;
    int MISFIRE_RESCHEDULE_REPEAT_COUNT = 2;
    int MISFIRE_RESCHEDULE_REMAINING_COUNT = 3;

    int REPEAT_INDEFINITELY = -1;

    int repeatCount();

    Interval repeatInterval();

    int timesTriggered();
}
