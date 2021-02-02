package inside.event.dispatcher;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildChannel;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.format.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.Consumer;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class CommonEvents extends Events{
    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private EntityRetriever entityRetriever;

    @Override
    public Publisher<?> onMessageClear(MessageClearEvent event){
        StringBuffer builder = new StringBuffer();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE));

        Consumer<Message> appendInfo = message -> {
            Member member = message.getAuthorAsMember().block();
            builder.append("[").append(formatter.print(message.getTimestamp().toEpochMilli())).append("] ");
            if(DiscordUtil.isBot(member)){
                builder.append("[BOT] ");
            }

            builder.append(DiscordUtil.detailName(member)).append(" > ");
            if(!MessageUtil.isEmpty(message)){
                builder.append(MessageUtil.effectiveContent(message));
            }

            for(int i = 0; i < message.getEmbeds().size(); i++){
                Embed e = message.getEmbeds().get(i);
                builder.append("\n[embed-").append(i).append(']');
                if(e.getDescription().isPresent()){
                    builder.append("\n").append(e.getDescription().get());
                }
            }
            builder.append("\n");
        };

        Mono<Void> publishLog = event.channel.map(GuildChannel::getName).flatMap(c -> log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format(context, "audit.message.clear.title", event.count, c));
            embed.setDescription(messageService.format(context, "audit.message.clear.description", event.member.getUsername(), event.count, c));
            embed.setFooter(timestamp(), null);
            embed.setColor(messageClear.color);
        }, true));

        return event.history
                .filter(Objects::nonNull)
                .sort(Comparator.comparing(Message::getId))
                .publishOn(Schedulers.boundedElastic())
                .onErrorResume(__ -> Mono.empty())
                .flatMap(m -> Mono.fromRunnable(() -> {
                    messageService.putMessage(m.getId());
                    appendInfo.accept(m);
                    m.delete().block();
                }))
                .then(Mono.fromRunnable(() -> stringInputStream.writeString(builder.toString())).then(publishLog));
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        LocalMember local = event.localMember;
        return event.guild().getMemberById(local.userId())
                .filter(member -> !entityRetriever.muteDisabled(member.getGuildId()))
                .flatMap(member -> {
                    Mono<Void> unmute = adminService.unmute(local.guildId(), local.userId())
                            .then(member.removeRole(entityRetriever.muteRoleId(member.getGuildId())));

                    Mono<Void> publishLog = log(member.getGuildId(), embed -> {
                        embed.setTitle(messageService.get(context, "audit.member.unmute.title"));
                        embed.setDescription(messageService.format(context, "audit.member.unmute.description", member.getUsername()));
                        embed.setFooter(timestamp(), null);
                        embed.setColor(userUnmute.color);
                    });

                    return unmute.then(publishLog);
                });
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE));

        LocalMember local = event.target;
        Guild guild = event.guild();
        return guild.getMemberById(local.userId())
                .filter(member -> !entityRetriever.muteDisabled(member.getGuildId()))
                .flatMap(member -> {
                    Mono<Member> admin = guild.getMemberById(event.admin.userId());

                    Mono<Void> mute = adminService.isMuted(member.getGuildId(), member.getId()).flatMap(b -> b ? member.addRole(entityRetriever.muteRoleId(member.getGuildId())) : Mono.fromRunnable(() -> {
                        adminService.mute(event.admin, local, event.delay.toCalendar(context.get(KEY_LOCALE)), event.reason().orElse(null)).block();
                        member.addRole(entityRetriever.muteRoleId(member.getGuildId())).block();
                    }));

                    Mono<Void> publishLog = admin.map(Member::getUsername).flatMap(username -> log(member.getGuildId(), embed -> {
                        embed.setTitle(messageService.get(context, "audit.member.mute.title"));
                        embed.setDescription(String.format("%s%n%s%n%s",
                        messageService.format(context, "audit.member.mute.description", member.getUsername(), username),
                        messageService.format(context, "common.reason", event.reason().orElse(messageService.get(context, "common.not-defined"))),
                        messageService.format(context, "audit.member.mute.delay", formatter.print(event.delay))
                        ));
                        embed.setFooter(timestamp(), null);
                        embed.setColor(userMute.color);
                    }));

                    return mute.then(publishLog);
                });
    }
}
