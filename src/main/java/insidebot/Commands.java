package insidebot;

import arc.Events;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.CommandHandler.*;
import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import insidebot.EventType.*;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.*;

import java.util.List;

import static insidebot.InsideBot.*;

public class Commands{
    private final String prefix = "$";
    private final CommandHandler handler = new CommandHandler(prefix);
    private final String[] warningStrings = {bundle.get("command.first"), bundle.get("command.second"), bundle.get("command.third")};

    public Commands(){
        handler.register("help", bundle.get("command.help.description"), args -> {
            StringBuilder builder = new StringBuilder();

            handler.getCommandList().forEach(command -> {
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0){
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append('*');
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append('\n');
            });
            listener.info(bundle.get("command.help"), builder.toString());
        });

        handler.register("mute", "<@user> <delayDays> [reason...]", bundle.get("command.mute.description"), (args, messageInfo) -> {
            if(!MessageUtil.canParseInt(args[1])){
                listener.err(bundle.get("command.incorrect-number"));
                return;
            }

            try{
                int delayDays = Strings.parseInt(args[1]);
                UserInfo info = UserInfoDao.get(MessageUtil.parseUserId(args[0]));
                User user = info.asUser();

                if(isAdmin(listener.guild.getMemberById(user.getId()).block())){
                    listener.err(bundle.get("command.user-is-admin"));
                    return;
                }
                if(user.isBot()){
                    listener.err(bundle.get("command.user-is-bot"));
                    return;
                }
                if(listener.lastUser == user){
                    listener.err(bundle.get("command.mute.self-user"));
                    return;
                }

                Events.fire(new MemberMuteEvent(info, delayDays));
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });

        handler.register("delete", "<amount>", bundle.get("command.delete.description"), args -> {
            if(!MessageUtil.canParseInt(args[0])){
                listener.err(bundle.get("command.incorrect-number"));
                return;
            }

            int number = Integer.parseInt(args[0]) + 1;

            if(number >= 100){
                listener.err(bundle.format("command.limit-number", 100));
                return;
            }

            List<Message> history = listener.channel.getMessagesBefore(listener.lastMessage.getId())
                                                    .limitRequest(number)
                                                    .collectList()
                                                    .block();

            if(history == null || (history.isEmpty() && number > 0)){
                listener.err(bundle.get("command.hist-error"));
                return;
            }

            Events.fire(new EventType.MessageClearEvent(history, listener.lastUser, listener.channel, number));
            history.forEach(m -> m.delete().block());
        });

        handler.register("warn", "<@user> [reason...]", bundle.get("command.warn.description"), args -> {
            try{
                UserInfo info = UserInfoDao.get(MessageUtil.parseUserId(args[0]));
                User user = info.asUser();

                if(isAdmin(listener.guild.getMemberById(user.getId()).block())){
                    listener.err(bundle.get("command.user-is-admin"));
                    return;
                }
                if(user.isBot()){
                    listener.err(bundle.get("command.user-is-bot"));
                    return;
                }
                if(listener.lastUser == user){
                    listener.err(bundle.get("command.warn.self-user"));
                    return;
                }

                int warnings = info.addWarn();

                listener.text(bundle.format("message.warn", user.getUsername(),
                                            warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]));

                if(warnings >= 3){
                    Events.fire(new MemberBanEvent(info));
                }else{
                    UserInfoDao.update(info);
                }
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });

        handler.register("warnings", "<@user>", bundle.get("command.warnings.description"), args -> {
            try{
                UserInfo info = UserInfoDao.get(MessageUtil.parseUserId(args[0]));
                int warnings = info.getWarns();

                listener.text(bundle.format("command.warnings", info.getName(), warnings,
                                            warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns")));
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });

        handler.register("unwarn", "<@user> [count]", bundle.get("command.unwarn.description"), args -> {
            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                listener.text(bundle.get("command.incorrect-number"));
                return;
            }

            int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;

            try{
                Snowflake l = MessageUtil.parseUserId(args[0]);
                UserInfo info = UserInfoDao.get(l);
                info.setWarns(info.getWarns() - warnings);

                listener.text(bundle.format("command.unwarn", info.getName(), warnings,
                                            warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns")));
                UserInfoDao.update(info);
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });

        handler.register("unmute", "<@user>", bundle.get("command.unmute.description"), args -> {
            try{
                UserInfo info = UserInfoDao.get(MessageUtil.parseUserId(args[0]));
                Events.fire(new MemberUnmuteEvent(info));
                Events.fire(new MemberUnmuteEvent(info));
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
    }

    public void handle(MessageCreateEvent event){
        if(event.getMessage().getContent().startsWith(prefix)){
            listener.channel = event.getMessage().getChannel().cast(TextChannel.class).block();
            listener.lastUser = event.getMessage().getAuthor().get();
            listener.lastMessage = event.getMessage();
        }

        if(isAdmin(event.getMember().get())){
            handleResponse(handler.handleMessage(event.getMessage().getContent()));
        }
    }

    public boolean isAdmin(Member member){
        try{
            return member != null && member.getRoles().map(Role::getPermissions).any(r -> r.contains(Permission.ADMINISTRATOR)).block();
        }catch(Throwable t){
            return false;
        }
    }

    void handleResponse(CommandResponse response){
        if(response.type == ResponseType.unknownCommand){
            listener.err(bundle.format("command.response.unknown", prefix));
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                listener.err(bundle.get("command.response.incorrect-arguments"),
                             bundle.format("command.response.incorrect-argument",
                                           prefix, response.command.text));
            }else{
                listener.err(bundle.get("command.response.incorrect-arguments"),
                             bundle.format("command.response.incorrect-arguments.text",
                                           prefix, response.command.text, response.command.paramText));
            }
        }
    }
}
