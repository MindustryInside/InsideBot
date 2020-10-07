package insidebot;

import arc.math.Mathf;
import arc.util.*;
import arc.util.CommandHandler.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static insidebot.InsideBot.bundle;
import static insidebot.InsideBot.listener;

public class Commands{
    private final String prefix = "$";
    private final CommandHandler handler = new CommandHandler(prefix);
    private final String[] warningStrings = {bundle.get("command.first"), bundle.get("command.second"), bundle.get("command.third")};

    public Commands(){
        handler.<MessageInfo>register("help", bundle.get("command.help.description"), (args, messageInfo) -> {
            StringBuilder builder = new StringBuilder();

            for(Command command : handler.getCommandList()){
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0){
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append("\n");
            }
            listener.info(bundle.get("command.help"), builder.toString());
        });
        handler.<MessageInfo>register("mute", "<@user> <delayDays> [reason...]", bundle.get("command.mute.description"), (args, messageInfo) -> {
            if(Strings.parseInt(args[1]) <= 0){
                listener.err(bundle.get("command.incorrect-number"));
                return;
            }

            try{
                long l = MessageUtil.parseUserId(args[0]);
                int delayDays = Strings.parseInt(args[1]);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = UserInfo.get(l);

                if(isAdmin(listener.guild.getMember(user))){
                    listener.err(bundle.get("command.user-is-admin"));
                    return;
                }else if(user.isBot()){
                    listener.err(bundle.get("command.user-is-bot"));
                    return;
                }else if(listener.lastUser == user){
                    listener.err(bundle.get("command.mute.self-user"));
                    return;
                }

                info.mute(delayDays);
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
        handler.<MessageInfo>register("delete", "<amount>", bundle.get("command.delete.description"), (args, messageInfo) -> {
            if(Strings.parseInt(args[0]) <= 0){
                listener.err(bundle.get("command.incorrect-number"));
                return;
            }

            int number = Integer.parseInt(args[0]) + 1;

            if(number >= 100){
                listener.err(bundle.get("command.limit-number"));
                return;
            }

            MessageHistory hist = listener.channel.getHistoryBefore(listener.lastMessage, number).complete();
            listener.onMessageClear(hist, listener.lastUser, number);
            listener.channel.deleteMessages(hist.getRetrievedHistory()).queue();
        });
        handler.<MessageInfo>register("warn", "<@user> [reason...]", bundle.get("command.warn.description"), (args, messageInfo) -> {
            try{
                long l = MessageUtil.parseUserId(args[0]);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = UserInfo.get(l);

                if(isAdmin(listener.guild.getMember(user))){
                    listener.err(bundle.get("command.user-is-admin"));
                    return;
                }else if(user.isBot()){
                    listener.err(bundle.get("command.user-is-bot"));
                    return;
                }else if(listener.lastUser == user){
                    listener.err(bundle.get("command.warn.self-user"));
                    return;
                }

                info.addWarns();

                int warnings = info.getWarns();

                listener.text(bundle.format("message.warn", user.getAsMention(),
                        warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]));

                if(warnings >= 3) info.ban();
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
        handler.<MessageInfo>register("warnings", "<@user>", bundle.get("command.warnings.description"), (args, messageInfo) -> {
            try{
                long l = MessageUtil.parseUserId(args[0]);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = UserInfo.get(l);
                int warnings = info.getWarns();
                listener.text(bundle.format("command.warnings", user.getName(), warnings,
                        warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns")));
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
        handler.<MessageInfo>register("unwarn", "<@user> [count]", bundle.get("command.unwarn.description"), (args, messageInfo) -> {
            if(args.length > 1 && Strings.parseInt(args[1]) <= 0){
                listener.text(bundle.get("command.incorrect-number"));
                return;
            }

            int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;

            try{
                long l = MessageUtil.parseUserId(args[0]);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = UserInfo.get(l);
                info.removeWarns(warnings);

                listener.text(bundle.format("command.unwarn", user.getName(), warnings,
                        warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns")));
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
        handler.<MessageInfo>register("unmute", "<@user>", bundle.get("command.unmute.description"), (args, messageInfo) -> {
            try{
                long l = MessageUtil.parseUserId(args[0]);
                UserInfo info = UserInfo.get(l);
                info.unmute();
            }catch(Exception e){
                listener.err(bundle.get("command.incorrect-name"));
            }
        });
    }

    public void handle(MessageReceivedEvent event, MessageInfo info){
        String text = event.getMessage().getContentRaw();

        if(event.getMessage().getContentRaw().startsWith(prefix)){
            listener.channel = event.getTextChannel();
            listener.lastUser = event.getAuthor();
            listener.lastMessage = event.getMessage();
        }

        if(isAdmin(event.getMember())){
            handleResponse(handler.handleMessage(text, info));
        }
    }

    public boolean isAdmin(Member member){
        try{
            return member.getRoles().stream().anyMatch(r -> r.hasPermission(Permission.ADMINISTRATOR));
        }catch(Exception e){
            return false;
        }
    }

    void handleResponse(CommandResponse response){
        if(response.type == ResponseType.unknownCommand){
            listener.err(bundle.format("command.response.unknown", prefix));
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                listener.err(bundle.get("command.response.incorrect-arguments"), bundle.format("command.response.incorrect-argument",
                             prefix, response.command.text));
            }else{
                listener.err(bundle.get("command.response.incorrect-arguments"), bundle.format("command.response.incorrect-arguments.text",
                             prefix, response.command.text, response.command.paramText));
            }
        }
    }
}
