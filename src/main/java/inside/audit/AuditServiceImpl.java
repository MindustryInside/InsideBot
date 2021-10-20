package inside.audit;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.service.EntityRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService{

    private final EntityRetriever entityRetriever;
    private Map<AuditActionType, AuditProvider> providers;

    public AuditServiceImpl(@Autowired EntityRetriever entityRetriever){
        this.entityRetriever = entityRetriever;
    }

    @Autowired(required = false)
    private void registerProviders(List<AuditProvider> providers){
        this.providers = providers.stream().collect(Collectors.toMap(
                p -> p.getClass().getAnnotation(ForwardAuditProvider.class).value(), Function.identity()
        ));
    }

    @Override
    @Transactional
    public Mono<Void> handle(AuditActionBuilder action, List<? extends Tuple2<String, InputStream>> attachments){
        AuditProvider forwardProvider = providers.get(action.getType());
        if(forwardProvider != null){
            return entityRetriever.getAuditConfigById(action.getGuildId())
                    .flatMap(config -> forwardProvider.send(config, action, attachments));
        }
        return Mono.error(new NoSuchElementException("Missed audit provider for type: " + action.getType()));
    }

    @Override
    public AuditActionBuilder newBuilder(Snowflake guildId, AuditActionType type){
        return new AuditActionBuilder(guildId, type){
            @Override
            public Mono<Void> save(){
                return handle(this, attachments == null ? Collections.emptyList() : attachments);
            }
        };
    }
}
