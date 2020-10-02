package insidebot;

import arc.files.Fi;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

import static insidebot.InsideBot.*;

public class Listener extends ListenerAdapter{

    public ObjectMap<Long, MetaInfo> messages = new ObjectMap<>();

    public Guild guild;
    public JDA jda;
    public Color normalColor = Color.decode("#C4F5B7");
    public Color errorColor = Color.decode("#ff3838");
    public Fi temp = new Fi("message.txt");

    TextChannel channel;
    User lastUser;
    Message lastMessage;
    Message lastSentMessage;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event){
        try{
            if(!event.getAuthor().isBot()){
                commands.handle(event);
                handleEvent(event, EventType.messageReceive);

                UserInfo info = data.getUserInfo(event.getAuthor().getIdLong());
                info.setLastMessageId(event.getMessageIdLong());
                info.setName(event.getAuthor().getName());
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event){
        try{
            if(!event.getAuthor().isBot()) handleEvent(event, EventType.messageEdit);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageDelete(@Nonnull MessageDeleteEvent event){
        try{
            handleEvent(event, EventType.messageDelete);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event){
        try{
            if(!event.getUser().isBot()) handleEvent(event, EventType.userJoin);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event){
        try{
            if(!event.getUser().isBot()) handleEvent(event, EventType.userLeave);
        }catch(Exception e){
            Log.err(e);
        }
    }

    public void text(String text, Object... args){
        lastSentMessage = channel.sendMessage(Strings.format(text, args)).complete();
    }

    public void info(String title, String text, Object... args){
        MessageEmbed object = new EmbedBuilder().setColor(normalColor)
                .setTitle(title).setDescription(Strings.format(text, args)).build();

        lastSentMessage = channel.sendMessage(object).complete();
    }

    public void embed(MessageEmbed embed){
        lastSentMessage = channel.sendMessage(embed).complete();
    }

    public void err(String text, Object... args){
        err(bundle.get("error"), text, args);
    }

    public void err(String title, String text, Object... args){
        MessageEmbed e = new EmbedBuilder().setColor(errorColor)
                .setTitle(title).setDescription(Strings.format(text, args)).build();

        lastSentMessage = channel.sendMessage(e).complete();
    }

    public void log(MessageEmbed embed){
        jda.getTextChannelById(logChannelID).sendMessage(embed).queue();
    }

    public void handleAction(Object object, ActionType type){
        EmbedBuilder builder = new EmbedBuilder().setColor(listener.normalColor);
        User user = (User) object;
        switch(type){
            case ban -> guild.ban(user, 0).queue();
            case mute -> guild.addRoleToMember(guild.getMember(user), jda.getRolesByName(muteRoleName, true).get(0)).queue();
            case unMute -> {
                builder.setTitle(bundle.get("message.unmute"));
                builder.setDescription(bundle.format("message.unmute.text", user.getName()));
                builder.setFooter(data.zonedFormat());

                listener.log(builder.build());
                guild.removeRoleFromMember(guild.getMember(user), jda.getRolesByName(muteRoleName, true).get(0)).queue();
            }
        }
    }

    private void writeTemp(String text){
        temp.writeString(text, false);
    }

    public void handleEvent(Object object, EventType type){
        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(normalColor);
        embedBuilder.setFooter(data.zonedFormat());

        int maxLength = 1024;
        switch(type){
            case messageReceive -> {
                MessageReceivedEvent event = (MessageReceivedEvent) object;
                MetaInfo info = new MetaInfo();
                StringBuilder c = new StringBuilder();
                info.text = event.getMessage().getContentRaw();
                info.id = event.getAuthor().getIdLong();

                if(!event.getMessage().getAttachments().isEmpty()){
                    info.file = event.getMessage().getAttachments();
                    c.append("\n---\n");
                    info.file.forEach(a -> c.append(a.getUrl()).append("\n"));
                    info.text += c.toString();
                }

                messages.put(event.getMessageIdLong(), info);
            }
            case messageEdit -> {
                MessageUpdateEvent event = (MessageUpdateEvent) object;
                boolean write = false;

                if(!messages.containsKey(event.getMessageIdLong()) | event.getMessage().isPinned()) return;

                MetaInfo info = messages.get(event.getMessageIdLong());

                String oldContent = info.text;
                String newContent = event.getMessage().getContentRaw();

                embedBuilder.setAuthor(memberedName(event.getAuthor()), null, event.getAuthor().getAvatarUrl());
                embedBuilder.setTitle(bundle.format("message.edit", event.getTextChannel().getName()));
                embedBuilder.setDescription(bundle.format("message.edit.description",
                                            event.getGuild().getId(), event.getTextChannel().getId(), event.getMessageId()));

                if(newContent.length() >= maxLength || oldContent.length() >= maxLength){
                    write = true;
                    writeTemp(String.format("%s\n%s\n\n%s\n%s", bundle.get("message.edit.old-content"), oldContent,
                                            bundle.get("message.edit.new-content"), newContent));
                }

                if(!event.getMessage().getAttachments().isEmpty()){
                    StringBuilder builder = new StringBuilder();
                    builder.append("\n---\n");
                    event.getMessage().getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
                    newContent += builder.toString();
                }

                embedBuilder.addField(bundle.get("message.edit.old-content"), oldContent.length() >= maxLength
                        ? (oldContent.substring(0, maxLength - 4) + "...") : oldContent, false);
                embedBuilder.addField(bundle.get("message.edit.new-content"), newContent.length() >= maxLength
                        ? (newContent.substring(0, maxLength - 4)) + "..." : newContent, true);

                if(write){
                    jda.getTextChannelById(logChannelID).sendMessage(embedBuilder.build()).addFile(temp.file()).queue();
                }else{
                    log(embedBuilder.build());
                }

                info.text = newContent;
            }
            case messageDelete -> {
                MessageDeleteEvent event = (MessageDeleteEvent) object;
                if(!messages.containsKey(event.getMessageIdLong())) return;
                MetaInfo info = messages.get(event.getMessageIdLong());

                User user = jda.retrieveUserById(info.id).complete();
                String content = info.text;

                if(user == null || content == null) return;

                embedBuilder.setAuthor(memberedName(user), null, user.getAvatarUrl());
                embedBuilder.setTitle(bundle.format("message.delete", event.getTextChannel().getName()));

                if(content.length() >= maxLength){
                    embedBuilder.addField(bundle.get("message.delete.content"), content.substring(0, maxLength - 4) + "...", true);
                    writeTemp(String.format("%s\n%s", bundle.get("message.delete.content"), content));
                    jda.getTextChannelById(logChannelID).sendMessage(embedBuilder.build()).addFile(temp.file()).queue();
                }else{
                    embedBuilder.addField(bundle.get("message.delete.content"), content, true);
                    log(embedBuilder.build());
                }

                messages.remove(event.getMessageIdLong());
            }
            case userJoin -> {
                GuildMemberJoinEvent event = (GuildMemberJoinEvent) object;

                embedBuilder.setTitle(bundle.get("message.user-join"));
                embedBuilder.setDescription(bundle.format("message.user-join.text", event.getUser().getName()));

                log(embedBuilder.build());
            }
            case userLeave -> {
                GuildMemberLeaveEvent event = (GuildMemberLeaveEvent) object;
                data.getUserInfo(event.getUser().getIdLong()).remove();

                embedBuilder.setTitle(bundle.get("message.user-leave"));
                embedBuilder.setDescription(bundle.format("message.user-leave.text", event.getUser().getName()));

                log(embedBuilder.build());
            }
        }
    }

    public EmbedBuilder createEmbed(Cons<EmbedBuilder> embed){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embed.get(embedBuilder);
        return embedBuilder;
    }

    public String memberedName(User user){
        String name = user.getName();
        Member member = guild.getMember(user);
        if(member.getNickname() != null){
            name += " / " + member.getNickname();
        }
        return name;
    }

    public enum ActionType{
        ban, mute, unMute,
    }

    public enum EventType{
        messageReceive,
        messageEdit,
        messageDelete,

        userJoin,
        userLeave
    }

    private static class MetaInfo{
        public String text;
        public long id;
        public List<Attachment> file;
    }
}
