package inside.event.audit;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.AuditActionRepository;
import inside.data.service.EntityRetriever;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.*;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService{
    private static final Logger log = Loggers.getLogger(AuditService.class);

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AuditActionRepository repository;

    @Autowired
    private Settings settings;

    private Map<AuditActionType, AuditProvider> providers;

    @Override
    @Transactional
    public Mono<Void> save(AuditAction action, List<Tuple2<String, InputStream>> attachments){
        GuildConfig config = entityRetriever.getGuildById(action.guildId());
        AuditProvider forwardProvider = providers.get(action.type());
        if(forwardProvider != null){
            if(false){ // I don't see the point in clogging up the database yet
                repository.save(action);
            }
            return forwardProvider.send(config, action, attachments);
        }
        return Mono.empty();
    }

    @Override
    public AuditActionBuilder log(Snowflake guildId, AuditActionType type){
        return new AuditActionBuilder(guildId, type){
            @Override
            public Mono<Void> save(){
                return AuditServiceImpl.this.save(action, attachments == null ? Collections.emptyList() : attachments);
            }
        };
    }

    @Override
    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void cleanUp(){
        repository.deleteByTimestampBefore(DateTime.now().minusWeeks(settings.historyExpireWeeks));
    }

    @Autowired(required = false)
    public void init(List<AuditProvider> providers){
        this.providers = providers.stream().collect(Collectors.toMap(
                p -> p.getClass().getAnnotation(ForwardAuditProvider.class).value(), p -> p
        ));
    }
}
