package insidebot;

import arc.Files;
import arc.files.Fi;
import arc.util.I18NBundle;
import arc.util.Log;
import insidebot.thread.Checker;
import insidebot.thread.Cleaner;
import net.dv8tion.jda.api.JDABuilder;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import javax.security.auth.login.LoginException;
import java.util.Locale;

public class InsideBot{

    public static final long guildID = 697929564210331681L;
    public static final long logChannelID = 747893115980873838L;
    public static final String muteRoleName = "muted";
    public static final String activeUserRoleName = "Active user";

    public static Listener listener;
    public static Commands commands;
    public static Config config;
    public static Database data;
    public static I18NBundle bundle;

    public static void main(String[] args) throws InterruptedException, LoginException{
        init();

        listener.jda = new JDABuilder(config.get("token"))
                .addEventListeners(listener)
                .build();
        listener.jda.awaitReady();

        listener.guild = listener.jda.getGuildById(guildID);

        Log.info("Discord bot up.");

        new Checker();
        new Cleaner();

        //ActiveUsers activeUsers = new ActiveUsers();   на время изменений
        //activeUsers.lastWipe = LocalDateTime.now().getDayOfYear();
    }

    private static void init(){
        listener = new Listener();
        config = new Config();

        Fi fi = new Fi("bundle", Files.FileType.classpath);

        try{
            bundle = I18NBundle.createBundle(fi, new Locale(config.get("locale")), "Windows-1251");
        }catch(Exception e){
            Log.err(e);
            bundle = I18NBundle.createBundle(fi, new Locale(""), "Windows-1251");
        }

        data = new Database();
        commands = new Commands();
    }

    public static class Config{

        private final Fi config = new Fi("config.json", Files.FileType.classpath);
        private JsonObject object;

        public Config(){
            try{
                object = JsonValue.readJSON(config.readString()).asObject();
            }catch(Exception e){
                Log.err(e);
            }
        }

        public String get(String key){
            try{
                object = JsonValue.readJSON(config.readString()).asObject();
                return object.get(key).asString();
            }catch(Exception e){
                return "";
            }
        }
    }
}
