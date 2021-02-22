package inside.event.dispatcher;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.format.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.util.function.Consumer;

import static inside.event.audit.AuditActionType.*;
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
                Embed embed = message.getEmbeds().get(i);
                builder.append("\n[embed-").append(i + 1).append("]");
                embed.getDescription().ifPresent(s -> builder.append("\n").append(s));
            }
            builder.append("\n");
        };

        Mono<Void> publishLog = log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format(context, "audit.message.clear.title", event.getCount(), event.getChannel().getName()));
            embed.setDescription(messageService.format(context, "audit.message.clear.description", event.getMember().getUsername(), event.getCount(), messageService.getCount(context, "common.plurals.message", event.getCount()), event.getChannel().getName()));
            embed.setFooter(timestamp(), null);
            embed.setColor(MESSAGE_CLEAR.color);
        }, true);

        return Flux.fromIterable(event.getHistory())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(message -> message.delete().then(Mono.fromRunnable(() -> {
                    messageService.putMessage(message.getId());
                    appendInfo.accept(message);
                })))
                .then(Mono.fromRunnable(() -> stringInputStream.writeString(builder.toString())).then(publishLog));
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        LocalMember local = event.getLocalMember();
        return event.guild().getMemberById(local.userId())
                .zipWhen(member -> Mono.justOrEmpty(entityRetriever.muteRoleId(member.getGuildId())))
                .flatMap(TupleUtils.function((member, roleId) -> {
                    Mono<Void> unmute = adminService.unmute(local.guildId(), local.userId())
                            .then(member.removeRole(roleId));

                    Mono<Void> publishLog = log(member.getGuildId(), embed -> embed.setTitle(messageService.get(context, "audit.member.unmute.title"))
                            .setDescription(messageService.format(context, "audit.member.unmute.description", member.getUsername()))
                            .setFooter(timestamp(), null)
                            .setColor(USER_UNMUTE.color));

                    return unmute.then(publishLog);
                }));
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE));

        LocalMember local = event.getTarget();
        Guild guild = event.guild();
        return guild.getMemberById(local.userId())
                .zipWhen(member -> Mono.justOrEmpty(entityRetriever.muteRoleId(member.getGuildId())))
                .flatMap(TupleUtils.function((member, roleId) -> {
                    Mono<Member> admin = guild.getMemberById(event.getAdmin().userId());

                    Mono<Void> mute = adminService.isMuted(member.getGuildId(), member.getId())
                            .flatMap(bool -> !bool ? adminService.mute(event.getAdmin(), local, event.getDelay().toCalendar(context.get(KEY_LOCALE)), event.reason().orElse(null)) : Mono.empty())
                            .then(member.addRole(roleId));

                    Mono<Void> publishLog = admin.flatMap(adminMember -> log(member.getGuildId(), embed -> {
                        embed.setTitle(messageService.get(context, "audit.member.mute.title"));
                        embed.setDescription(String.format("%s%n%s%n%s",
                        messageService.format(context, "audit.member.mute.description", member.getUsername(), adminMember.getUsername()),
                        messageService.format(context, "common.reason", event.reason().orElse(messageService.get(context, "common.not-defined"))),
                        messageService.format(context, "audit.member.mute.delay", formatter.print(event.getDelay()))
                        ));
                        embed.setFooter(timestamp(), null);
                        embed.setColor(USER_MUTE.color);
                    }));

                    return mute.then(publishLog);
                }));
    }
}
