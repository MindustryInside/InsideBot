package inside.event.dispatcher;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

import static inside.event.audit.AuditEventType.*;

@Service
public class CommonEvents extends Events{
    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MemberService memberService;

    @Override
    public Publisher<?> onMessageClear(MessageClearEvent event){
        StringBuffer builder = new StringBuffer();

        Consumer<Message> appendInfo = m -> {
            Member member = m.getAuthorAsMember().block();
            builder.append('[').append(MessageUtil.dateTime().withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
            if(DiscordUtil.isBot(member)){
                builder.append("[BOT] ");
            }
            builder.append(memberService.detailName(member)).append(" > ");
            if(!MessageUtil.isEmpty(m)) builder.append(MessageUtil.effectiveContent(m));
            for(int i = 0; i < m.getEmbeds().size(); i++){
                Embed e = m.getEmbeds().get(i);
                builder.append("\n[embed-").append(i).append(']');
                if(e.getDescription().isPresent()){
                    builder.append('\n').append(e.getDescription().get());
                }else{
                    builder.append("<empty>");
                }
            }
            builder.append('\n');
        };

        Flux.fromIterable(event.history)
            .filter(Objects::nonNull)
            .subscribe(m -> {
                messageService.putMessage(m.getId());
                appendInfo.accept(m);
                m.delete().block();
            }, e -> {});

        stringInputStream.writeString(builder.toString());

        return log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format("audit.message.clear.title", event.count, event.channel.getName()));
            embed.setDescription(messageService.format("audit.message.clear.description", event.user.getUsername(), event.count, event.channel.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.setColor(messageClear.color);
        }, true);
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        LocalMember l = event.localMember;
        Member member = event.guild().getMemberById(l.user().userId()).block();
        if(member == null) return Mono.empty();
        if(guildService.muteDisabled(member.getGuildId())) return Mono.empty();

        adminService.unmute(l.guildId(), l.user().userId()).block();
        member.removeRole(guildService.muteRoleId(member.getGuildId())).block();
        return log(member.getGuildId(), e -> {
            e.setTitle(messageService.get("audit.member.unmute.title"));
            e.setDescription(messageService.format("audit.member.unmute.description", member.getUsername()));
            e.setFooter(MessageUtil.zonedFormat(), null);
            e.setColor(userUnmute.color);
        });
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        LocalMember l = event.target;
        Member member = event.guild().getMemberById(l.user().userId()).block();
        if(member == null) return Mono.empty();
        if(guildService.muteDisabled(member.getGuildId())) return Mono.empty();

        Calendar end = Calendar.getInstance();
        end.roll(Calendar.DAY_OF_YEAR, +event.delay);
        adminService.mute(event.admin, l, end, event.reason().orElse(null)).block();
        member.addRole(guildService.muteRoleId(member.getGuildId())).block();

        return log(member.getGuildId(), e -> {
            e.setTitle(messageService.get("audit.member.mute.title"));
            e.setDescription(String.format("%s%n%s",
                                           messageService.format("audit.member.mute.description", member.getUsername(), event.delay, event.admin.username()),
                                           messageService.format("common.reason", event.reason().orElse(messageService.get("common.not-defined")))));
            e.setFooter(MessageUtil.zonedFormat(), null);
            e.setColor(userMute.color);
        });
    }
}
