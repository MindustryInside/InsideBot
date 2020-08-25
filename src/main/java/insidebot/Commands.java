package insidebot;

import arc.math.Mathf;
import arc.util.*;
import arc.util.CommandHandler.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static insidebot.InsideBot.*;

public class Commands{
    private final String prefix = "$";
    private final CommandHandler handler = new CommandHandler(prefix);
    private final String[] warningStrings = {"once", "twice", "thrice"};

    public Guild roleGuild;

    Commands() {
        handler.register("help", "Displays all bot commands.", args -> {
            StringBuilder builder = new StringBuilder();

            for (Command command : handler.getCommandList()) {
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if (command.params.length > 0) {
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append("\n");
            }
            listener.info("Commands", builder.toString());
        });
        handler.register("mute", "<@user> <delayDays> [reason...]", "Mute a user.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!")) author = author.substring(1);

            try {
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();

                listener.text("**{0}**, you've been warned *{1}*.", user.getAsMention(), warningStrings[Mathf.clamp(1- 1, 0, warningStrings.length - 1)]);

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(listener.normalColor);
                builder.addField("Mute", Strings.format("Пользователь **{0}** замьючен!", user.getName()), true);
                builder.setFooter(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now()));

                jda.getTextChannelById(logChannelID).sendMessage(builder.build()).queue();
                roleGuild.ban(user, 1, args[1]).queue();
            } catch (Exception e) {
                Log.err(e);
                listener.err("Incorrect name format.");
            }
        });
        handler.register("delete", "<amount>", "Delete some messages.", args -> {
            try {
                int number = Integer.parseInt(args[0]) + 1;
                MessageHistory hist = listener.channel.getHistoryBefore(listener.lastMessage, number).complete();
                listener.channel.deleteMessages(hist.getRetrievedHistory()).queue();
                Log.info("Deleted {0} messages.", number);
            } catch (NumberFormatException e) {
                listener.err("Invalid number.");
            }
        });
        handler.register("warn", "<@user> [reason...]", "Warn a user.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!")) author = author.substring(1);

            try {
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();

                data.addWarn(l);

                int warnings = data.getWarns(l);
                listener.text("**{0}**, you've been warned *{1}*.", user.getAsMention(), warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]);

                if (data.getWarns(l) >= 3) {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(listener.normalColor);
                    builder.addField("Ban", Strings.format("Пользователь **{0}** забанен!", user.getName()), true);
                    builder.setFooter(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now()));

                    jda.getTextChannelById(logChannelID).sendMessage(builder.build()).queue();
                    roleGuild.ban(user, 1, args[1]).queue();
                }
            } catch (Exception e) {
                Log.err(e);
                listener.err("Incorrect name format.");
            }
        });
        handler.register("warnings", "<@user>", "Get number of warnings a user has.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!")) author = author.substring(1);
            try {
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                int warnings = data.getWarns(l);
                listener.text("User '{0}' has **{1}** {2}.", user.getName(), warnings, warnings == 1 ? "warning" : "warnings");
            } catch (Exception e) {
                listener.err("Incorrect name format.");
            }
        });
        handler.register("unwarn", "<@user> [count]", "Unwarn a user.", args -> {
            if(args.length > 1 && !Strings.canParseInt(args[1])){
                listener.lastSentMessage.getTextChannel().sendMessage("'count' must be a integer!").queue();
                return;
            }

            String author = args[0].substring(2, args[0].length() - 1);
            int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;
            if (author.startsWith("!")) author = author.substring(1);

            try {
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                data.removeWarns(l, warnings);

                listener.text("{0} was removed from {1}", warnings > 1 ? "warnings" : "Warning", user.getName());
            } catch (Exception e) {
                listener.err("Incorrect name format.");
            }
        });
    }

    void handle(MessageReceivedEvent event){
        String text = event.getMessage().getContentRaw();
        roleGuild = event.getGuild();

        if(event.getMessage().getContentRaw().startsWith(prefix)){
            listener.channel = event.getTextChannel();
            listener.lastUser = event.getAuthor();
            listener.lastMessage = event.getMessage();
        }

        if(isAdmin(event.getMember())){
            handleResponse(handler.handleMessage(text));
        }
    }

    boolean isAdmin(Member member) {
        try {
            return member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("moderator") || r.getName().equalsIgnoreCase("ULTRAPOWERED SUPERSKAT"));
        } catch (Exception e) {
            return false;
        }
    }

    void handleResponse(CommandResponse response){
        if(response.type == ResponseType.unknownCommand){
            listener.err("Неизвестная команда. Type " + prefix + "help для получения списка команд.");
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                listener.err("Invalid arguments.", "Usage: {0}{1}", prefix, response.command.text);
            }else{
                listener.err("Invalid arguments.", "Usage: {0}{1} *{2}*", prefix, response.command.text, response.command.paramText);
            }
        }
    }
}
