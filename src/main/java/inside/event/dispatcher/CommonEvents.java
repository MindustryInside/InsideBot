package inside.event.dispatcher;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.format.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

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
        DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                                                    .withLocale(context.locale());

        Consumer<Message> appendInfo = m -> {
            Member member = m.getAuthorAsMember().block();
            builder.append('[').append(formatter.print(m.getTimestamp().toEpochMilli())).append("] ");
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

        event.history.filter(Objects::nonNull)
             .subscribe(m -> { // todo пока не придумал как сделать неблокируемо
                 messageService.putMessage(m.getId());
                 appendInfo.accept(m);
                 m.delete().block();
             }, e -> {});

        stringInputStream.writeString(builder.toString());
        String channel = event.channel.map(GuildChannel::getName).block();

        return log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format("audit.message.clear.title", event.count, channel));
            embed.setDescription(messageService.format("audit.message.clear.description", event.user.getUsername(), event.count, channel));
            embed.setFooter(timestamp(), null);
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
            e.setFooter(timestamp(), null);
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
        end.setTime(event.delay.toDate());
        adminService.mute(event.admin, l, end, event.reason().orElse(null)).block();
        member.addRole(guildService.muteRoleId(member.getGuildId())).block();

        return log(member.getGuildId(), e -> {
            e.setTitle(messageService.get("audit.member.mute.title"));
            e.setDescription(String.format("%s%n%s",
            messageService.format("audit.member.mute.description", member.getUsername(), event.delay, event.admin.username()),
            messageService.format("common.reason", event.reason().orElse(messageService.get("common.not-defined")))));
            e.setFooter(timestamp(), null);
            e.setColor(userMute.color);
        });
    }
}
