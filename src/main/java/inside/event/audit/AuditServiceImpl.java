package inside.event.audit;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.AuditActionRepository;
import inside.data.service.EntityRetriever;
import inside.util.ContextUtil;
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

    private Map<AuditEventType, AuditForwardProvider> providers;

    @Override
    @Transactional
    public Mono<Void> save(AuditAction action, List<Tuple2<String, InputStream>> attachments){
        GuildConfig config = entityRetriever.getGuildById(action.guildId());
        AuditForwardProvider forwardProvider = providers.get(action.type());
        if(forwardProvider != null){
            repository.save(action);
            return forwardProvider.send(config, action, attachments);
        }
        return Mono.empty();
    }

    @Override
    public AuditActionBuilder log(Snowflake guildId, AuditEventType type){
        return new AuditActionBuilder(guildId, type){
            @Override
            public Mono<Void> save(){
                return AuditServiceImpl.this.save(action, attachments);
            }
        };
    }

    @Override
    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void cleanUp(){
        repository.deleteByTimestampBefore(DateTime.now().minusWeeks(settings.historyExpireWeeks).toCalendar(Locale.getDefault()));
    }

    @Autowired(required = false)
    public void init(List<AuditForwardProvider> providers){
        this.providers = providers.stream().collect(Collectors.toMap(
                e -> e.getClass().getAnnotation(AuditProvider.class).value(), e -> e
        ));
    }
}
