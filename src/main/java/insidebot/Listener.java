package insidebot;

import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectSet;
import arc.util.*;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.domain.*;
import discord4j.core.event.domain.guild.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.VoiceState;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import discord4j.rest.util.Color;
import insidebot.EventType.*;
import insidebot.data.entity.*;
import insidebot.data.services.*;
import insidebot.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import static insidebot.audit.AuditEventType.*;
import static insidebot.InsideBot.*;

public class Listener{
    public Guild guild;
    public DiscordClient client;
    public GatewayDiscordClient gateway;
    public Color normalColor = Color.of(0xC4F5B7), errorColor = Color.of(0xff3838);
    public Fi temp = new Fi("message.txt");

    // привет костыль (на самом деле не совсем то и костыль)
    public ObjectSet<Snowflake> buffer = new ObjectSet<>();

    TextChannel channel;
    User lastUser;
    Message lastMessage, lastSentMessage;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Logger log;

    protected void register(){
        guild = gateway.getGuildById(guildID).block();
        // Внутренние ивенты

        Events.on(MemberUnmuteEvent.class, event -> {
            Member member = event.userInfo.asMember().block();
            if(DiscordUtil.isBot(member)) return;

            event.userInfo.muteEndDate(null);
            userService.delete(event.userInfo);
            member.removeRole(muteRoleID).block();
            log(e -> {
                e.setTitle(messageService.get("message.unmute"));
                e.setDescription(messageService.format("message.unmute.text", event.userInfo.name()));
                e.setFooter(MessageUtil.zonedFormat(), null);
                e.setColor(userUnmute.color);
            });
        });

        Events.on(MemberMuteEvent.class, event -> {
            Member member = guild.getMemberById(event.user.getId()).block();
            UserInfo userInfo = userService.getById(event.user.getId());
            if(DiscordUtil.isBot(member) || userInfo == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
            userInfo.muteEndDate(calendar);
            userService.save(userInfo);
            member.addRole(muteRoleID).block();
            log(embedBuilder -> {
                embedBuilder.setTitle(messageService.get("message.mute"));
                embedBuilder.setDescription(messageService.format("message.mute.text", event.user.getMention(), event.delay));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
                embedBuilder.setColor(userMute.color);
            });
        });

        Events.on(MessageClearEvent.class, event -> {
            Flux.fromIterable(event.history).subscribe(m -> {
                buffer.add(m.getId());
                m.delete().block();
            });

            log(embed -> {
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
                temp.writeString(builder.toString());
            }, true);
        });
    }

    // utils

    public void text(String text, Object... args){
        lastSentMessage = channel.createMessage(Strings.format(text, args)).block();
    }

    public void info(String title, String text, Object... args){
        lastSentMessage = channel.createMessage(s -> s.setEmbed(e -> e.setColor(normalColor).setTitle(title)
                                                                      .setDescription(Strings.format(text, args)))).block();
    }

    public void err(String text, Object... args){
        err(messageService.get("error"), text, args);
    }

    public void err(String title, String text, Object... args){
        lastSentMessage = channel.createMessage(s -> s.setEmbed(e -> e.setColor(errorColor).setTitle(title)
                                                                      .setDescription(Strings.format(text, args)))).block();
    }

    public void log(Consumer<EmbedCreateSpec> embed){
        log(embed, false);
    }

    public void log(Consumer<EmbedCreateSpec> embed, boolean file){
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        log(file ? m.addFile("message.txt", temp.read()) : m);
    }

    public void log(@NonNull MessageCreateSpec message){
        guild.getChannelById(logChannelID).cast(TextChannel.class).block()
             .getRestChannel().createMessage(message.asRequest()).block();
    }
}
