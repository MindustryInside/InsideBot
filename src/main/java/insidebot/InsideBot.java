package insidebot;

import discord4j.common.util.Snowflake;
import org.springframework.boot.SpringApplication;

public class InsideBot{
    public static final Snowflake
    guildID = Snowflake.of(697929564210331681L),
    logChannelID = Snowflake.of(747893115980873838L),
    muteRoleID = Snowflake.of(747910443816976568L),
    activeUserRoleID = Snowflake.of(697939241308651580L); // TODO УБРАТЬ ОТСЮДА ЭТО, БУДЕТ СПЕЦ. ОБЪЕКТ ДЛЯ ЭТОГО

    public static void main(String[] args){
        SpringApplication.run(InsideBot.class, args);
    }
}
