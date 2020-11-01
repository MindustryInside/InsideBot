package insidebot;

import arc.files.Fi;
import arc.func.Cons;
import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Strings;
import insidebot.data.dao.MessageInfoDao;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.MessageInfo;
import insidebot.data.model.UserInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.util.Calendar;

import static insidebot.AuditEventType.*;
import static insidebot.InsideBot.*;

public class Listener extends ListenerAdapter{
    public Guild guild;
    public JDA jda;
    public Color normalColor = Color.decode("#C4F5B7");
    public Color errorColor = Color.decode("#ff3838");
    public Fi temp = new Fi("message.txt");

    // привет костыль
    public ObjectSet<Long> buffer = new ObjectSet<>();

    TextChannel channel;
    User lastUser;
    Message lastMessage;
    Message lastSentMessage;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event){
        try{
            if(event.getAuthor().isBot()) return;
            StringBuilder c = new StringBuilder();
            MessageInfo info = new MessageInfo();
            UserInfo userInfo = UserInfoDao.getOr(event.getAuthor().getIdLong(), UserInfo::new);
            String content = event.getMessage().getContentRaw().trim();

            userInfo.setName(event.getAuthor().getName());
            userInfo.setUserId(event.getAuthor().getIdLong());
            userInfo.setLastSentMessage(Calendar.getInstance());
            userInfo.addToSeq();

            info.setUser(userInfo);
            info.setMessageId(event.getMessageIdLong());
            info.setChannelId(event.getTextChannel().getIdLong());

            if(!event.getMessage().getAttachments().isEmpty()){
                c.append("\n---\n");
                event.getMessage().getAttachments().forEach(a -> c.append(a.getUrl()).append("\n"));
                content += c.toString();
            }

            info.setContent(content);
            userInfo.getMessageInfo().add(info);

            commands.handle(event, info);
            UserInfoDao.saveOrUpdate(userInfo);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event){
        try{
            if(event.getAuthor().isBot()) return;
            EmbedBuilder embedBuilder = new EmbedBuilder();
            boolean write = false;

            if(!MessageInfoDao.repo().containsKey(event.getMessageIdLong())) return;

            MessageInfo info = MessageInfoDao.repo().get(event.getMessageIdLong());

            User user = event.getAuthor();
            String oldContent = info.getContent();
            String newContent = event.getMessage().getContentRaw();
            int maxLength = 1024;

            if(event.getMessage().isPinned() || newContent.equals(oldContent)) return;

            embedBuilder.setColor(messageEdit.color);
            embedBuilder.setAuthor(memberedName(user), null, user.getEffectiveAvatarUrl());
            embedBuilder.setTitle(bundle.format("message.edit", event.getTextChannel().getName()));
            embedBuilder.setDescription(bundle.format("message.edit.description",
                                                      event.getGuild().getId(),
                                                      event.getTextChannel().getId(),
                                                      event.getMessageId()));

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

            embedBuilder.addField(bundle.get("message.edit.old-content"),
                    MessageUtil.substringTo(oldContent, maxLength), false);
            embedBuilder.addField(bundle.get("message.edit.new-content"),
                    MessageUtil.substringTo(newContent, maxLength), true);

            embedBuilder.setFooter(data.zonedFormat());

            if(write){
                jda.getTextChannelById(logChannelID).sendMessage(embedBuilder.build()).addFile(temp.file()).queue();
            }else{
                log(embedBuilder);
            }

            info.setContent(newContent);
            MessageInfoDao.update(info);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageDelete(@Nonnull MessageDeleteEvent event){
        try{
            if(!MessageInfoDao.repo().containsKey(event.getMessageIdLong())) return;
            if(buffer.contains(event.getMessageIdLong())){
                buffer.remove(event.getMessageIdLong());
                return;
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();
            MessageInfo info = MessageInfoDao.repo().get(event.getMessageIdLong());

            User user = info.getUser().asUser();
            String content = info.getContent();
            int maxLength = 1024;

            if(user == null || content == null || content.isEmpty()) return;

            embedBuilder.setColor(messageDelete.color);
            embedBuilder.setAuthor(memberedName(user), null, user.getEffectiveAvatarUrl());
            embedBuilder.setTitle(bundle.format("message.delete", event.getTextChannel().getName()));
            embedBuilder.setFooter(data.zonedFormat());

            if(content.length() >= maxLength){
                embedBuilder.addField(bundle.get("message.delete.content"), MessageUtil.substringTo(content, maxLength), true);
                writeTemp(String.format("%s\n%s", bundle.get("message.delete.content"), content));
                log(embedBuilder, temp.file());
            }else{
                embedBuilder.addField(bundle.get("message.delete.content"), content, true);
                log(embedBuilder);
            }

            MessageInfoDao.remove(info);
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event){
        try{
            User user = event.getMember().getUser();
            if(user.isBot()) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceJoin.color);
                embedBuilder.setTitle(bundle.get("message.voice-join"));
                embedBuilder.setDescription(bundle.format("message.voice-join.text", memberedName(user),
                                                          event.getChannelJoined().getName()));
                embedBuilder.setFooter(data.zonedFormat());
            });
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event){
        try{
            User user = event.getMember().getUser();
            if(user.isBot()) return;
            log(embedBuilder -> {
                embedBuilder.setColor(voiceLeave.color);
                embedBuilder.setTitle(bundle.get("message.voice-leave"));
                embedBuilder.setDescription(bundle.format("message.voice-leave.text", memberedName(user),
                                                          event.getChannelLeft().getName()));
                embedBuilder.setFooter(data.zonedFormat());
            });
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event){
        try{
            if(event.getUser().isBot()) return;
            log(embedBuilder -> {
                embedBuilder.setColor(userJoin.color);
                embedBuilder.setTitle(bundle.get("message.user-join"));
                embedBuilder.setDescription(bundle.format("message.user-join.text", event.getUser().getName()));
                embedBuilder.setFooter(data.zonedFormat());
            });
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event){
        try{
            if(event.getUser().isBot()) return;
            UserInfoDao.removeById(event.getUser().getIdLong());
            log(embedBuilder -> {
                embedBuilder.setColor(userLeave.color);
                embedBuilder.setTitle(bundle.get("message.user-leave"));
                embedBuilder.setDescription(bundle.format("message.user-leave.text", event.getUser().getName()));
                embedBuilder.setFooter(data.zonedFormat());
            });
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onGuildBan(@Nonnull GuildBanEvent event){
        try{
            if(event.getUser().isBot()) return;
            UserInfoDao.removeById(event.getUser().getIdLong());
            log(embedBuilder -> {
                embedBuilder.setColor(userBan.color);
                embedBuilder.setTitle(bundle.get("message.ban"));
                embedBuilder.setDescription(bundle.format("message.ban.text", event.getUser().getName()));
                embedBuilder.setFooter(data.zonedFormat());
            });
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onUserUpdateName(@Nonnull UserUpdateNameEvent event){
        try{
            if(event.getEntity().isBot()) return;
            UserInfo info = UserInfoDao.get(event.getEntity().getIdLong());
            if(info == null) return;
            info.setName(event.getNewName());
            UserInfoDao.update(info);
        }catch(Exception e){
            Log.err(e);
        }
    }

    // voids

    public void onMessageClear(MessageHistory history, User user, int count){
        log(embedBuilder -> {
            embedBuilder.setTitle(bundle.format("message.clear", count, history.getChannel().getName()));
            embedBuilder.setDescription(bundle.format("message.clear.text", user.getAsMention(),
                                                      count, history.getChannel().getName()));
            embedBuilder.setFooter(data.zonedFormat());
            embedBuilder.setColor(messageClear.color);

            StringBuilder builder = new StringBuilder();
            history.getRetrievedHistory().forEach(m -> buffer.add(m.getIdLong()));
            history.getRetrievedHistory().forEach(m -> {
                builder.append('[').append(m.getTimeCreated()).append("] ");
                builder.append(user.getName()).append(" = ");
                builder.append(m.getContentRaw());
                if(!m.getAttachments().isEmpty()){
                    builder.append("\n---\n");
                    m.getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
                }
                builder.append("\n");
            });
            writeTemp(builder.toString());
        }, temp.file());
    }

    public void onMemberMute(User user, int delayDays){
        Member member = guild.getMember(user);
        UserInfo userInfo = UserInfoDao.get(user.getIdLong());
        if(member == null || userInfo == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, +delayDays);
        userInfo.setMuteEndDate(calendar);
        UserInfoDao.update(userInfo);
        guild.addRoleToMember(member, muteRole).queue();
        log(embedBuilder -> {
            embedBuilder.setTitle(bundle.get("message.mute"));
            embedBuilder.setDescription(bundle.format("message.mute.text", user.getAsMention(), delayDays));
            embedBuilder.setFooter(data.zonedFormat());
            embedBuilder.setColor(userMute.color);
        });
    }

    public void onMemberUnmute(UserInfo userInfo){
        Member member = userInfo.asMember();
        if(member == null) return;

        userInfo.setMuteEndDate(null);
        UserInfoDao.update(userInfo);
        guild.removeRoleFromMember(member, muteRole).queue();
        log(embedBuilder -> {
            embedBuilder.setTitle(bundle.get("message.unmute"));
            embedBuilder.setDescription(bundle.format("message.unmute.text", userInfo.getName()));
            embedBuilder.setFooter(data.zonedFormat());
            embedBuilder.setColor(userUnmute.color);
        });
    }

    // utils

    public void ban(UserInfo userInfo){
        listener.guild.ban(userInfo.asUser(), 0).queue();
        UserInfoDao.remove(userInfo);
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

    public void log(EmbedBuilder embed){
        jda.getTextChannelById(logChannelID).sendMessage(embed.build()).queue();
    }

    public void log(Cons<EmbedBuilder> embed){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embed.get(embedBuilder);
        log(embedBuilder);
    }

    public void log(EmbedBuilder embed, File temp){
        jda.getTextChannelById(logChannelID).sendMessage(embed.build()).addFile(temp).queue();
    }

    public void log(Cons<EmbedBuilder> embed, File temp){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embed.get(embedBuilder);
        log(embedBuilder, temp);
    }

    private void writeTemp(String text){
        temp.writeString(text, false);
    }

    // username / membername
    public String memberedName(User user){
        String name = user.getName();
        Member member = guild.getMember(user);
        if(member != null && member.getNickname() != null){
            name += " / " + member.getNickname();
        }
        return name;
    }
}
