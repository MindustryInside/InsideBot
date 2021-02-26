package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.*;
import reactor.core.publisher.Mono;

public interface DiscordService{

    GatewayDiscordClient gateway();

    Mono<TextChannel> getTextChannelById(Snowflake channelId);

    // void activeUsersMonitor();
}
