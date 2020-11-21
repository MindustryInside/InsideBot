package insidebot;

import arc.files.Fi;
import arc.struct.*;
import arc.util.I18NBundle;
import arc.util.Log;
import arc.util.io.PropertiesUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import insidebot.thread.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

import static arc.Files.FileType.classpath;
import static arc.util.Log.format;

public class InsideBot{
    public static final String prefix = "$";
    public static final Snowflake
    guildID = Snowflake.of(697929564210331681L),
    logChannelID = Snowflake.of(747893115980873838L),
    muteRoleID = Snowflake.of(747910443816976568L),
    activeUserRoleID = Snowflake.of(697939241308651580L);

    public static ScheduledExecutorService executorService;
    public static StringMap settings = new StringMap();

    public static Listener listener;
    public static Commands commands;
    public static Database data;
    public static I18NBundle bundle;

    protected static String[] tags = {"&lc&fb[D]", "&lg&fb[I]", "&ly&fb[W]", "&lr&fb[E]", ""};
    protected static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    public static void main(String[] args){
        init();

        listener.client = DiscordClient.create(settings.get("token"));
        listener.gateway = listener.client.login().block();
        listener.register();

        executorService.scheduleAtFixedRate(new Unmuter(), 5, 15, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new ActiveUsers(), 10, 60, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(new AuditCleaner(), 15, 12, TimeUnit.HOURS);

        listener.gateway.onDisconnect().block();
    }

    private static void init(){
        // да, с миндастри взял
        Log.logger = (level, text) -> {
            String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);
        };

        listener = new Listener();
        executorService = Executors.newScheduledThreadPool(3);

        Fi cfg = new Fi("settings.properties", classpath);
        Fi fi = new Fi("bundle", classpath);
        PropertiesUtils.load(settings, cfg.reader());
        if(settings.getBool("debug")){
            Log.level = Log.LogLevel.debug;
        }

        bundle = I18NBundle.createBundle(fi, new Locale(settings.get("locale", "en")), "Windows-1251");
        data = new Database();
        commands = new Commands();
    }
}
