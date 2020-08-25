package insidebot;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Strings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;

import static insidebot.InsideBot.*;

public class Listener extends ListenerAdapter{
    ObjectMap<Long, MetaInfo> messages = new ObjectMap<>();

    TextChannel channel;
    User lastUser;
    Guild guild;
    Message lastMessage;
    Message lastSentMessage;

    Color normalColor = Color.decode("#C4F5B7");
    Color errorColor = Color.decode("#ff3838");

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        try{
            if (!event.getAuthor().isBot()) {
                commands.handle(event);
                handleEvent(event, EventType.messageReceive);
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
        try{
            if (!event.getAuthor().isBot() ) {
                handleEvent(event, EventType.messageEdit);
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
        try{
            handleEvent(event, EventType.messageDelete);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        try{
            if(!event.getUser().isBot()) handleEvent(event, EventType.userJoin);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        try{
            if(!event.getUser().isBot()) handleEvent(event, EventType.userLeave);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(@Nonnull GuildMemberUpdateNicknameEvent event) {
        try{
            if(!event.getUser().isBot()) handleEvent(event, EventType.userNameEdit);
        }catch(Exception e){
            Log.err(e);
        }
    }

    public void unMute(long id){
        // TODO Допилить
    }

    public void mute(long id){
        // TODO Допилить
    }

    public void text(String text, Object... args){
        lastSentMessage = channel.sendMessage(Strings.format(text, args)).complete();
    }

    public void info(String title, String text, Object... args){
        MessageEmbed object = new EmbedBuilder()
        .addField(title, Strings.format(text, args), true).setColor(normalColor).build();

        lastSentMessage = channel.sendMessage(object).complete();
    }

    public void err(String text, Object... args){
        err("Error", text, args);
    }

    public void err(String title, String text, Object... args){
        MessageEmbed e = new EmbedBuilder()
        .addField(title, Strings.format(text, args), true).setColor(errorColor).build();
        lastSentMessage = channel.sendMessage(e).complete();
    }

    public void send(String title, String text, Object... args){
        MessageEmbed e = new EmbedBuilder()
                .addField(title, Strings.format(text, args), true)
                .setColor(normalColor).build();
        jda.getTextChannelById(logChannelID).sendMessage(e).queue();
    }

    public void handleEvent(Object object, EventType type){
        switch (type) {
            case messageReceive -> {
                MessageReceivedEvent event = (MessageReceivedEvent) object;
                MetaInfo info = new MetaInfo();
                info.text = event.getMessage().getContentRaw();
                info.id = event.getAuthor().getIdLong();

                messages.put(event.getMessageIdLong(), info);
            }case messageEdit -> {
                MessageUpdateEvent event = (MessageUpdateEvent) object;
                MetaInfo info = messages.get(event.getMessageIdLong());

                send("MESSAGE EDIT", "`{0}` изменил сообщение `{1}` на `{2}`", event.getAuthor().getName(), info.text, event.getMessage().getContentRaw());
                info.text = event.getMessage().getContentRaw();
            }case messageDelete -> {
                MessageDeleteEvent event = (MessageDeleteEvent) object;
                MetaInfo info = messages.get(event.getMessageIdLong());

                send("MESSADE DELETE", "`{0}` удалил сообщение `{1}`", jda.retrieveUserById(info.id).complete().getName(), info.text);
                messages.remove(event.getMessageIdLong());
            }case userJoin -> {
                GuildMemberJoinEvent event = (GuildMemberJoinEvent) object;

                send("USER JOIN", "`{0}` присоединился", event.getUser().getName());
            }case userLeave -> {
                GuildMemberLeaveEvent event = (GuildMemberLeaveEvent) object;

                send("USER LEAVE", "`{0}` ушёл с сервера", event.getUser().getName());
            }case userNameEdit -> {
                GuildMemberUpdateNicknameEvent event = (GuildMemberUpdateNicknameEvent) object;

                send("USERNAME EDIT", "`{0}` сменил ник на `{1}`", event.getOldNickname(), event.getNewNickname());
            }
        }
    }

    public enum EventType{
        messageEdit,
        messageDelete,
        messageReceive,

        userNameEdit,
        userJoin,
        userLeave,
    }

    public static class MetaInfo{
        public String text;
        public long id;
    }
}
