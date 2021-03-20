package inside.data.service.impl;

import discord4j.core.object.entity.Member;
import inside.Settings;
import inside.data.entity.LocalMember;
import inside.data.repository.LocalMemberRepository;
import inside.data.service.BaseEntityService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.Nullable;

@Service
public class MemberServiceImpl extends BaseEntityService<Member, LocalMember, LocalMemberRepository>{

    protected MemberServiceImpl(@Autowired LocalMemberRepository repository,
                                @Autowired Settings settings){
        super(repository, settings);
    }

    @Override
    protected LocalMember create(Member id){
        LocalMember localMember = new LocalMember();
        localMember.userId(id.getId());
        localMember.guildId(id.getGuildId());
        localMember.effectiveName(id.getDisplayName());
        return localMember;
    }

    @Nullable
    @Override
    @Transactional
    protected LocalMember get(Member id){
        String guildId = id.getGuildId().asString();
        String userId = id.getId().asString();
        return repository.findByGuildIdAndUserId(guildId, userId);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanUp(){
        repository.deleteByLastSentMessageBefore(DateTime.now().minus(settings.getAudit().getMemberKeep().toMillis()));
    }
}
