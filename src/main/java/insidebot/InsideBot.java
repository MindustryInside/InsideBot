package insidebot;

import arc.Files;
import arc.files.Fi;
import arc.util.*;
import insidebot.thread.Checker;
import insidebot.thread.ClearThread;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import java.util.Locale;

public class InsideBot {
    public static final long logChannelID = 747893115980873838L;
    public static final String muteRoleName = "muted";
    public static final String activeUserRoleName = "Active user";
    public static final long guildID = 747805212366077953L;

    public static JDA jda;

    public static Listener listener;
    public static Commands commands;
    public static Config config;
    public static Database data;
    public static I18NBundle bundle;

    public static void main(String[] args) throws InterruptedException, LoginException {
        init();

        jda = new JDABuilder(config.get("token"))
                .addEventListeners(listener)
                .build();
        jda.awaitReady();

        listener.guild = jda.getGuildById(guildID);

        Log.info("Discord bot up.");

        new Checker();
        new ClearThread();
    }

    public static void init(){
        listener = new Listener();
        config = new Config();

        try {
            bundle = I18NBundle.createBundle(new Fi("bundle", Files.FileType.classpath), new Locale(config.get("locale")), "Windows-1251");
        } catch (Exception e){
            Log.err(e);
            bundle = I18NBundle.createBundle(new Fi("bundle", Files.FileType.classpath), new Locale(""), "Windows-1251");
        }

        data = new Database();
        commands = new Commands();
    }
}
