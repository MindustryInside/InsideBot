package inside.data.service.impl;

import discord4j.store.api.util.LongLongTuple2;
import inside.Settings;
import inside.data.entity.LocalMember;
import inside.data.repository.LocalMemberRepository;
import inside.data.service.BaseEntityService;
import org.joda.time.DateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalMemberService extends BaseEntityService<LongLongTuple2, LocalMember, LocalMemberRepository>{

    protected LocalMemberService(LocalMemberRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected LocalMember find0(LongLongTuple2 id){
        long userId = id.getT1();
        long guildId = id.getT2();
        return repository.findByUserIdAndGuildId(userId, guildId);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanUp(){
        repository.deleteByLastSentMessageBefore(DateTime.now().minus(settings.getAudit().getMemberKeep().toMillis()));
    }
}
