package insidebot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import org.springframework.boot.SpringApplication;

public class InsideBot{
    public static final String prefix = "$";
    public static final Snowflake
    guildID = Snowflake.of(697929564210331681L),
    logChannelID = Snowflake.of(747893115980873838L),
    muteRoleID = Snowflake.of(747910443816976568L),
    activeUserRoleID = Snowflake.of(697939241308651580L); // TODO УБРАТЬ ОТСЮДА ЭТО, БУДЕТ СПЕЦ. ОБЪЕКТ ДЛЯ ЭТОГО

    public static Listener listener;
    public static Commands commands;

    public static void main(String[] args){
        init();

        listener.client = DiscordClient.create(""/*settings.get("token")*/); // temp
        listener.gateway = listener.client.login().block();
        listener.register();

        listener.gateway.onDisconnect().block();

        // TODO
        // Доделать инициализацию объектов и прочего
        SpringApplication.run(InsideBot.class, args);
    }

    private static void init(){
        listener = new Listener();
        commands = new Commands();
    }
}
