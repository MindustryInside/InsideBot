package insidebot.common.services;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.*;
import insidebot.event.dispatcher.EventListener;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public interface DiscordService{

    GatewayDiscordClient gateway();

    EventListener eventListener();

    Mono<TextChannel> getTextChannelById(@NonNull Snowflake guildId);

    Mono<VoiceChannel> getVoiceChannelById(@NonNull Snowflake guildId);
}
