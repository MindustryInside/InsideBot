package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.LocalMember;
import inside.data.repository.LocalMemberRepository;
import inside.data.service.MemberService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService{

    private final LocalMemberRepository repository;

    private final Settings settings;

    public MemberServiceImpl(@Autowired LocalMemberRepository repository,
                             @Autowired Settings settings){
        this.repository = repository;
        this.settings = settings;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Snowflake guildId, Snowflake userId){
        return repository.findByGuildIdAndUserId(guildId.asString(), userId.asString());
    }

    @Override
    @Transactional
    public void save(LocalMember member){
        repository.save(member);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanUp(){
        repository.deleteByLastSentMessageBefore(DateTime.now().minus(settings.getAudit().getMemberKeep().toMillis()));
    }
}
