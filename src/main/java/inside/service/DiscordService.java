package inside.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.core.publisher.Mono;

public interface DiscordService extends InteractionCommandHandler{

    GatewayDiscordClient gateway();

    Mono<TextChannel> getTextChannelById(Snowflake channelId);

    // void activeUsersMonitor();
}
