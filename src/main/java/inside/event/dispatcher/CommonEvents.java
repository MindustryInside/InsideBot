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
                                                    .withLocale(context.locale())
                                                    .withZone(context.zone());

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
             .subscribe(m -> { // todo пока не придумал как сделать не блокируемо
                 messageService.putMessage(m.getId());
                 appendInfo.accept(m);
                 m.delete().block();
             }, e -> {});

        stringInputStream.writeString(builder.toString());

        return event.channel.map(GuildChannel::getName).flatMap(c -> log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format("audit.message.clear.title", event.count, c));
            embed.setDescription(messageService.format("audit.message.clear.description", event.member.getUsername(), event.count, c));
            embed.setFooter(timestamp(), null);
            embed.setColor(messageClear.color);
        }, true));
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        LocalMember l = event.localMember;
        return event.guild().getMemberById(l.user().userId())
                .filter(m -> !guildService.muteDisabled(m.getGuildId()))
                .flatMap(m -> {
                    Mono<Void> unmute = Mono.fromRunnable(() -> {
                        adminService.unmute(l.guildId(), l.user().userId()).block();
                        m.removeRole(guildService.muteRoleId(m.getGuildId())).block();
                    });

                    Mono<Void> publishLog = log(m.getGuildId(), embed -> {
                        embed.setTitle(messageService.get("audit.member.unmute.title"));
                        embed.setDescription(messageService.format("audit.member.unmute.description", m.getUsername()));
                        embed.setFooter(timestamp(), null);
                        embed.setColor(userUnmute.color);
                    });

                    return unmute.then(publishLog);
                });
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                                                    .withLocale(context.locale())
                                                    .withZone(context.zone());
        LocalMember l = event.target;
        return event.guild().getMemberById(l.user().userId())
                .filter(member -> !guildService.muteDisabled(member.getGuildId()))
                .flatMap(m -> {
                    Mono<Void> mute = Mono.fromRunnable(() -> {
                        adminService.mute(event.admin, l, event.delay.toCalendar(context.locale()), event.reason().orElse(null)).block();
                        m.addRole(guildService.muteRoleId(m.getGuildId())).block();
                    });

                    Mono<Void> publishLog = log(m.getGuildId(), embed -> {
                        embed.setTitle(messageService.get("audit.member.mute.title"));
                        embed.setDescription(String.format("%s%n%s%n%s",
                        messageService.format("audit.member.mute.description", m.getUsername(), event.admin.username()),
                        messageService.format("common.reason", event.reason().orElse(messageService.get("common.not-defined"))),
                        messageService.format("audit.member.mute.delay", formatter.print(event.delay))
                        ));
                        embed.setFooter(timestamp(), null);
                        embed.setColor(userMute.color);
                    });

                    return mute.then(publishLog);
                });
    }
}
