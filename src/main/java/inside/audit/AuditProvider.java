package inside.audit;

import inside.data.entity.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.List;

public interface AuditProvider{

    Mono<Void> send(AuditConfig config, AuditAction action, List<? extends Tuple2<String, InputStream>> attachments);
}
