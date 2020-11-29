package insidebot.event.dispatcher.impl;

import discord4j.core.object.Embed;
import discord4j.core.object.entity.Member;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.LocalMember;
import insidebot.data.service.*;
import insidebot.event.MessageEventHandler;
import insidebot.event.dispatcher.EventType.*;
import insidebot.event.dispatcher.Events;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.time.ZoneId;
import java.util.*;

import static insidebot.audit.AuditEventType.*;

@Service
public class EventsImpl extends Events{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageEventHandler messageEventHandler;

    @Autowired
    private Logger log;

    @Override
    public Publisher<?> onMessageClear(MessageClearEvent event){
        context.init(event.guild().getId());
        Flux.fromIterable(event.history).filter(Objects::nonNull).subscribe(m -> {
            messageEventHandler.buffer.add(m.getId());
            m.delete().block();
        }, e -> log.warn("An exception was thrown in the MessageClear event, error: {}", e.getMessage()));

        return log(event.guild().getId(), embed -> {
            embed.setTitle(messageService.format("message.clear", event.count, event.channel.getName()));
            embed.setDescription(messageService.format("message.clear.text", event.user.getUsername(), event.count, event.channel.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.setColor(messageClear.color);

            StringBuilder builder = new StringBuilder();
            event.history.forEach(m -> {
                Member member = m.getAuthorAsMember().block();
                builder.append('[').append(MessageUtil.dateTime().withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
                if(!DiscordUtil.isBot(member)){
                    builder.append("[BOT] ");
                }
                builder.append(memberService.detailName(member)).append(" > ");
                if(!MessageUtil.isEmpty(m.getContent())) builder.append(MessageUtil.effectiveContent(m));
                for(int i = 0; i < m.getEmbeds().size(); i++){
                    Embed e = m.getEmbeds().get(i);
                    builder.append("\n[embed-").append(i).append(']');
                    if(e.getDescription().isPresent()){
                        builder.append('\n').append(e.getDescription().get());
                    }
                }
                builder.append('\n');
            });

            stringInputStream.writeString(builder.toString());
        }, true);
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        context.init(event.guild().getId());
        LocalMember l = event.localMember;
        Member member = event.guild().getMemberById(l.id()).block();
        if(member == null) return Mono.empty();

        l.muteEndDate(null);
        member.removeRole(guildService.muteRoleId(member.getGuildId())).block();
        memberService.save(l);
        return log(member.getGuildId(), e -> {
            e.setTitle(messageService.get("message.unmute"));
            e.setDescription(messageService.format("message.unmute.text", member.getUsername()));
            e.setFooter(MessageUtil.zonedFormat(), null);
            e.setColor(userUnmute.color);
        });
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        context.init(event.guild().getId());
        LocalMember l = event.localMember;
        Member member = event.guild().getMemberById(l.id()).block();
        if(member == null) return Mono.empty();

        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
        l.muteEndDate(calendar);
        memberService.save(l);
        member.addRole(guildService.muteRoleId(member.getGuildId())).block();

        return log(member.getGuildId(), embedBuilder -> {
            embedBuilder.setTitle(messageService.get("message.mute"));
            embedBuilder.setDescription(messageService.format("message.mute.text", member.getUsername(), event.delay));
            embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            embedBuilder.setColor(userMute.color);
        });
    }
}
