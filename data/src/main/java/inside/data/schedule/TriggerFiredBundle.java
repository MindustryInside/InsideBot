package inside.data.schedule;

import reactor.util.annotation.Nullable;

import java.time.Instant;

public record TriggerFiredBundle(JobDetail jobDetail, Trigger trigger, boolean recovering,
                                 Instant fireTimestamp, @Nullable Instant scheduledFireTimestamp,
                                 @Nullable Instant prevFireTime, @Nullable Instant nextFireTime) {}
