package insidebot.event;

import discord4j.core.event.domain.guild.MemberUpdateEvent;
import insidebot.data.entity.LocalMember;
import insidebot.data.service.MemberService;
import insidebot.util.DiscordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MemberUpdateHandler implements EventHandler<MemberUpdateEvent>{
    @Autowired
    private MemberService memberService;

    @Override
    public Class<MemberUpdateEvent> type(){
        return MemberUpdateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MemberUpdateEvent event){
        return event.getMember()
                    .filter(m -> !DiscordUtil.isBot(m))
                    .filter(m -> memberService.exists(m.getGuildId(), m.getId()))
                    .doOnNext(m -> {
                        LocalMember info = memberService.get(event.getGuildId(), m.getId());
                        info.effectiveName(m.getUsername());
                        memberService.save(info);
                    })
                    .then();
    }
}
