package insidebot.event.dispatcher.impl;

import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.LocalMember;
import insidebot.data.service.*;
import insidebot.event.MessageDeleteHandler;
import insidebot.event.dispatcher.*;
import insidebot.event.dispatcher.EventType.*;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.util.annotation.NonNull;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

import static insidebot.InsideBot.*;
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
    private MessageDeleteHandler messageDeleteHandler;

    private StringInputStream stringInputStream = new StringInputStream();

    @Override
    public Publisher<?> onMessageClear(MessageClearEvent event){
        Flux.fromIterable(event.history).filter(Objects::nonNull).subscribe(m -> {
            messageDeleteHandler.buffer.add(m.getId());
            m.delete().block();
        });

        return log(embed -> {
            embed.setTitle(messageService.format("message.clear", event.count, event.channel.getName()));
            embed.setDescription(messageService.format("message.clear.text", event.user.getUsername(), event.count, event.channel.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.setColor(messageClear.color);

            StringBuilder builder = new StringBuilder();
            event.history.forEach(m -> {
                builder.append('[').append(MessageUtil.dateTime().withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
                builder.append(m.getUserData().username()).append(" > ");
                builder.append(m.getContent());
                if(!m.getAttachments().isEmpty()){
                    builder.append("\n---\n");
                    m.getAttachments().forEach(a -> builder.append(a.getUrl()).append('\n'));
                }
                builder.append('\n');
            });

            stringInputStream.writeString(builder.toString());
        }, true);
    }

    @Override
    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        LocalMember l = event.localMember;
        Member member = event.guild().getMemberById(l.id()).block();
        if(DiscordUtil.isBot(member)) return Mono.empty();

        l.muteEndDate(null);
        member.removeRole(muteRoleID).block();
        memberService.save(l);
        return log(e -> {
            e.setTitle(messageService.get("message.unmute"));
            e.setDescription(messageService.format("message.unmute.text", member.getUsername()));
            e.setFooter(MessageUtil.zonedFormat(), null);
            e.setColor(userUnmute.color);
        });
    }

    @Override
    public Publisher<?> onMemberMute(MemberMuteEvent event){
        LocalMember l = event.localMember;
        Member member = event.guild().getMemberById(l.id()).block();
        if(DiscordUtil.isBot(member)) return Mono.empty();

        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
        l.muteEndDate(calendar);
        memberService.save(l);
        member.addRole(muteRoleID).block();

        return log(embedBuilder -> {
            embedBuilder.setTitle(messageService.get("message.mute"));
            embedBuilder.setDescription(messageService.format("message.mute.text", member.getUsername(), event.delay));
            embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            embedBuilder.setColor(userMute.color);
        });
    }

    //todo
    public Mono<Void> log(@NonNull MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }

    public Mono<Void> log(Consumer<EmbedCreateSpec> embed, boolean file){
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        return log(file ? m.addFile("message.txt", stringInputStream) : m);
    }

    public Mono<Void> log(Consumer<EmbedCreateSpec> embed){
        return log(embed, false);
    }
}
