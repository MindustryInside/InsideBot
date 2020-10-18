package insidebot;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.I18NBundle;
import arc.util.Log;
import arc.util.io.PropertiesUtils;
import insidebot.thread.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;

import javax.security.auth.login.LoginException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

import static arc.Files.FileType.classpath;
import static arc.util.Log.format;

public class InsideBot{
    static String[] tags = {"&lc&fb[D]", "&lg&fb[I]", "&ly&fb[W]", "&lr&fb[E]", ""};
    static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    public static final long guildID = 697929564210331681L;
    public static final long logChannelID = 747893115980873838L;
    public static Role muteRole = null;
    public static Role activeUserRole = null;

    public static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
    public static ObjectMap<String, String> settings = new ObjectMap<>();

    public static Listener listener;
    public static Commands commands;
    public static Database data;
    public static I18NBundle bundle;

    public static void main(String[] args) throws InterruptedException, LoginException{
        init();

        listener.jda = new JDABuilder(settings.get("token"))
                .addEventListeners(listener)
                .build();
        listener.jda.awaitReady();
        listener.guild = listener.jda.getGuildById(guildID);
        muteRole = listener.guild.getRoleById(747910443816976568L);
        activeUserRole = listener.guild.getRoleById(697939241308651580L);

        Log.info("Bot up.");

        executorService.scheduleAtFixedRate(new Unmuter(), 5, 15, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new ActiveUsers(), 10, 60, TimeUnit.SECONDS);
    }

    private static void init(){
        // да, с миндастри взял
        Log.setLogger((level, text) -> {
            String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);
        });

        listener = new Listener();

        Fi cfg = new Fi("settings.properties", classpath);
        Fi fi = new Fi("bundle", classpath);
        PropertiesUtils.load(settings, cfg.reader());

        bundle = I18NBundle.createBundle(fi, new Locale(settings.get("locale", "en")), "Windows-1251");
        data = new Database();
        commands = new Commands();
    }
}
