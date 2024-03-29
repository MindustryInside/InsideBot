package inside.data.service.impl;

import discord4j.store.api.util.LongLongTuple2;
import inside.Settings;
import inside.data.entity.LocalMember;
import inside.data.repository.LocalMemberRepository;
import inside.data.service.BaseEntityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Instant;

@Service
public class LocalMemberService extends BaseEntityService<LongLongTuple2, LocalMember, LocalMemberRepository>{

    private final Settings settings;

    protected LocalMemberService(LocalMemberRepository repository, Settings settings){
        super(repository, settings.getCache().isLocalMember());
        this.settings = settings;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    protected LocalMember find0(LongLongTuple2 id){
        long userId = id.getT1();
        long guildId = id.getT2();
        return repository.findByUserIdAndGuildId(userId, guildId);
    }

    @Override
    protected Object extractId(LocalMember entity){
        return LongLongTuple2.of(entity.getUserId().asLong(), entity.getGuildId().asLong());
    }

    @Transactional
    public Mono<Void> deleteAllByGuildId(long guildId){
        return Mono.fromRunnable(() -> repository.deleteAllByGuildId(guildId));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    protected void cleanUp(){
        repository.deleteAllByActivityLastSentMessageBefore(Instant.now().minus(settings.getAudit().getMemberKeep()));
    }
}
