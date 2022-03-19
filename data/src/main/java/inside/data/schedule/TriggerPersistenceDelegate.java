package inside.data.schedule;

import inside.data.api.r2dbc.R2dbcConnection;
import reactor.core.publisher.Mono;

public interface TriggerPersistenceDelegate {

    boolean canHandle(Trigger trigger);

    String getTypeDiscriminator();

    Mono<Integer> storeExtendedTriggerProperties(R2dbcConnection con, Trigger trigger, Trigger.State state);

    Mono<? extends Trigger> selectTriggerWithProperties(R2dbcConnection con, Key triggerKey);
}
