package inside.audit;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.List;

public interface AuditService{

    Mono<Void> handle(AuditActionBuilder action, List<? extends Tuple2<String, InputStream>> attachments);

    AuditActionBuilder newBuilder(Snowflake guildId, AuditActionType type);
}
