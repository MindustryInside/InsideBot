package insidebot;

import arc.math.Mathf;
import arc.util.*;
import arc.util.CommandHandler.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static insidebot.InsideBot.bundle;
import static insidebot.InsideBot.listener;

public class Commands{
    private final String prefix = "$";
    private final CommandHandler handler = new CommandHandler(prefix);
    private final String[] warningStrings = {bundle.get("command.first"), bundle.get("command.second"), bundle.get("command.third")};

    Commands(){
        handler.register("help", "Displays all bot commands.", args -> {
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
            listener.info("$command.list", builder.toString());
        });
        handler.register("mute", "<@user> <delayDays> [reason...]", "Mute a user.", args -> {
            if(Strings.parseInt(args[0]) <= 0){
                listener.err("$command.incorrect-number");
                return;
            }

            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);

            try{
                long l = Long.parseLong(author);
                int delayDays = Strings.parseInt(args[1]);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = InsideBot.data.getUserInfo(l);

                if(isAdmin(listener.guild.getMember(user))){
                    listener.err("$command.user-is-admin");
                    return;
                }else if(user.isBot()){
                    listener.err("$command.user-is-bot");
                    return;
                }else if(listener.lastUser == user){
                    listener.err("$command.self-user");
                    return;
                }

                EmbedBuilder builder = new EmbedBuilder().setColor(listener.normalColor);
                builder.addField(bundle.get("message.mute"), bundle.format("$message.mute.text", user.getAsMention(), delayDays), false);
                builder.setFooter(InsideBot.data.zonedFormat());

                listener.log(builder.build());
                info.mute(delayDays);
            }catch(Exception e){
                Log.err(e);
                listener.err("$command.incorrect-name");
            }
        });
        handler.register("delete", "<amount>", "Delete some messages.", args -> {
            if(Strings.parseInt(args[0]) <= 0){
                listener.err("$command.incorrect-number");
                return;
            }

            int number = Integer.parseInt(args[0]) + 1;

            if(number >= 100){
                listener.err("$command.limit-number");
                return;
            }

            MessageHistory hist = listener.channel.getHistoryBefore(listener.lastMessage, number).complete();
            listener.channel.deleteMessages(hist.getRetrievedHistory()).queue();
            Log.info("Deleted {0} messages.", number);
        });
        handler.register("warn", "<@user> [reason...]", "Warn a user.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);

            try{
                long l = Long.parseLong(author);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = InsideBot.data.getUserInfo(l);

                if(isAdmin(listener.guild.getMember(user))){
                    listener.err("$command.user-is-admin");
                    return;
                }else if(user.isBot()){
                    listener.err("$command.user-is-bot");
                    return;
                }else if(listener.lastUser == user){
                    listener.err("$command.self-user");
                    return;
                }

                info.addWarns();

                int warnings = info.getWarns();

                listener.text("$message.warn", user.getAsMention(), warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]);

                if(info.getWarns() >= 3){
                    EmbedBuilder builder = new EmbedBuilder().setColor(listener.normalColor);
                    builder.addField(bundle.get("message.ban"), bundle.format("message.ban.text", user.getName()), true);
                    builder.setFooter(InsideBot.data.zonedFormat());

                    listener.log(builder.build());
                    info.ban();
                }
            }catch(Exception e){
                listener.err("$command.incorrect-name");
            }
        });
        handler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);

            try{
                long l = Long.parseLong(author);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = InsideBot.data.getUserInfo(l);
                int warnings = info.getWarns();
                listener.text("$command.warnings", user.getName(), warnings,
                        warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns"));
            }catch(Exception e){
                listener.err("$command.incorrect-name");
            }
        });
        handler.register("unwarn", "<@user> [count]", "Unwarn a user.", args -> {
            if(args.length > 1 && Strings.parseInt(args[1]) <= 0){
                listener.text("$command.incorrect-number");
                return;
            }

            String author = args[0].substring(2, args[0].length() - 1);
            int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;
            if(author.startsWith("!")) author = author.substring(1);

            try{
                long l = Long.parseLong(author);
                User user = listener.jda.retrieveUserById(l).complete();
                UserInfo info = InsideBot.data.getUserInfo(l);
                info.removeWarns(warnings);

                listener.text("$command.unwarn", user.getName(), warnings, warnings == 1 ? bundle.get("command.warn") : bundle.get("command.warns"));
            }catch(Exception e){
                listener.err("$command.incorrect-name");
            }
        });
        handler.register("unmute", "<@user>", "Unmute a user.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);

            try{
                long l = Long.parseLong(author);
                UserInfo info = InsideBot.data.getUserInfo(l);
                info.unmute();
            }catch(Exception e){
                listener.err("$command.incorrect-name");
            }
        });
    }

    void handle(MessageReceivedEvent event){
        String text = event.getMessage().getContentRaw();

        if(event.getMessage().getContentRaw().startsWith(prefix)){
            listener.channel = event.getTextChannel();
            listener.lastUser = event.getAuthor();
            listener.lastMessage = event.getMessage();
        }

        if(isAdmin(event.getMember())){
            handleResponse(handler.handleMessage(text));
        }
    }

    boolean isAdmin(Member member){
        try{
            return member.getRoles().stream().anyMatch(r -> r.hasPermission(Permission.ADMINISTRATOR));
        }catch(Exception e){
            return false;
        }
    }

    void handleResponse(CommandResponse response){
        if(response.type == ResponseType.unknownCommand){
            listener.err("$command.response.unknown", prefix);
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                listener.err("$command.response.incorrect-arguments", bundle.format("command.response.incorrect-argument",
                        prefix, response.command.text));
            }else{
                listener.err("$command.response.incorrect-arguments", bundle.format("command.response.incorrect-arguments.text",
                        prefix, response.command.text, response.command.paramText));
            }
        }
    }
}
