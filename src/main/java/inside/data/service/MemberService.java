package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.LocalMember;

public interface MemberService{

    LocalMember get(Snowflake guildId, Snowflake userId);

    default LocalMember get(Member member){
        return get(member.getGuildId(), member.getId());
    }

    void save(LocalMember member);
}
