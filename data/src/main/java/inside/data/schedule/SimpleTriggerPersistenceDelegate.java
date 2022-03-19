package inside.data.schedule;

import inside.data.api.r2dbc.R2dbcConnection;
import inside.data.api.r2dbc.R2dbcResult;
import io.r2dbc.postgresql.codec.Interval;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

public class SimpleTriggerPersistenceDelegate implements TriggerPersistenceDelegate {

    private final String schedulerName;

    public SimpleTriggerPersistenceDelegate(String schedulerName) {
        this.schedulerName = Objects.requireNonNull(schedulerName, "schedulerName");
    }

    @Override
    public String getTypeDiscriminator() {
        return "simple";
    }

    @Override
    public boolean canHandle(Trigger trigger) {
        return trigger instanceof SimpleTrigger;
    }

    @Override
    public Mono<Integer> storeExtendedTriggerProperties(R2dbcConnection con, Trigger trigger, Trigger.State state) {
        SimpleTrigger trigger0 = (SimpleTrigger) trigger;

        return con.createStatement("insert into simple_trigger (scheduler_name, trigger_group, trigger_name, " +
                "repeat_count, repeat_interval, times_triggered) values ($1, $2, $3, $4, $5, $6) on conflict " +
                "(scheduler_name, trigger_group, trigger_name) do update set " +
                "repeat_count = $4, repeat_interval = $5, times_triggered = $6")
                .bind(0, schedulerName)
                .bind(1, trigger.key().group())
                .bind(2, trigger.key().name())
                .bind(3, trigger0.repeatCount())
                .bind(4, trigger0.repeatInterval())
                .bind(5, trigger0.timesTriggered())
                .execute()
                .flatMap(R2dbcResult::getRowsUpdated)
                .singleOrEmpty();
    }

    @Override
    public Mono<? extends SimpleTrigger> selectTriggerWithProperties(R2dbcConnection con, Key triggerKey) {
        return con.createStatement("""
                        select * from trigger as t left join
                          simple_trigger as s using (scheduler_name, trigger_group, trigger_name)
                            where t.scheduler_name = $1 and t.trigger_group = $2 and t.trigger_name = $3""")
                .bind(0, schedulerName)
                .bind(1, triggerKey.group())
                .bind(2, triggerKey.name())
                .execute()
                .flatMap(result -> result.mapWith(row -> {
                    Key jobKey = Key.of(row.getRequiredValue(3, String.class),
                            row.getRequiredValue(4, String.class));
                    Instant nextFireTimestamp = row.get(5, Instant.class);
                    Instant prevFireTimestamp = row.get(6, Instant.class);
                    int priority = row.getInt(7);
                    int misfireInstruction = row.getInt(8);
                    Instant startTimestamp = row.getRequiredValue(11, Instant.class);
                    Instant endTimestamp = row.get(12, Instant.class);

                    int repeatCount = row.getInt(13);
                    Interval repeatInterval = row.getRequiredValue(14, Interval.class);
                    int timesTriggered = row.getInt(15);

                    SimpleTriggerImpl trigger = new SimpleTriggerImpl(triggerKey, priority,
                            startTimestamp, misfireInstruction,
                            endTimestamp, repeatCount, repeatInterval);

                    trigger.setJobKey(jobKey);
                    trigger.setTimesTriggered(timesTriggered);
                    trigger.setNextFireTimestamp(nextFireTimestamp);
                    trigger.setPreviousFireTimestamp(prevFireTimestamp);

                    return trigger;
                }))
                .single();
    }
}
