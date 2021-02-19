package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.data.entity.LocalMember;
import inside.data.repository.LocalMemberRepository;
import inside.data.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService{

    private final LocalMemberRepository repository;

    public MemberServiceImpl(@Autowired LocalMemberRepository repository){
        this.repository = repository;
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
}
