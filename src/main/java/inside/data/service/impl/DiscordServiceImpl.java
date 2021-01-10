package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.*;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.*;
import discord4j.rest.response.ResponseFunction;
import inside.Settings;
import inside.data.service.DiscordService;
import inside.data.service.DiscordEntityRetrieveService;
import inside.event.dispatcher.EventListener;
import inside.event.dispatcher.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import javax.annotation.PreDestroy;
import java.util.*;

@Service
public class DiscordServiceImpl implements DiscordService{
    @Autowired
    private Settings settings;

    @Autowired
    private DiscordEntityRetrieveService discordEntityRetrieveService;

    protected GatewayDiscordClient gateway;

    protected EventListener eventListener;

    @Autowired(required = false)
    public void init(List<ReactiveEventAdapter> handlers, List<Events> events){
        String token = settings.token;
        Objects.requireNonNull(token, "Discord token not provided");

        gateway = DiscordClientBuilder.create(token)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .build()
                .gateway()
                .setMemberRequestFilter(MemberRequestFilter.all())
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES,
                        Intent.DIRECT_MESSAGE_REACTIONS
                ))
                .login()
                .block();

        eventListener = EventListener.buffering();

        Flux.fromIterable(events)
            .filter(Objects::nonNull)
            .subscribe(e -> eventListener.on(e).subscribe());

        Flux.fromIterable(handlers)
            .filter(Objects::nonNull)
            .subscribe(e -> gateway.on(e).subscribe());
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
    }

    @Override
    public GatewayDiscordClient gateway(){
        return gateway;
    }

    @Override
    public EventListener eventListener(){
        return eventListener;
    }

    @Override
    public Mono<PrivateChannel> getPrivateChannelById(Snowflake userId){
        return gateway.getUserById(userId).flatMap(User::getPrivateChannel);
    }

    @Override
    public Mono<TextChannel> getTextChannelById(Snowflake channelId){
        return gateway.getChannelById(channelId).cast(TextChannel.class);
    }

    @Override
    public Mono<TextChannel> getLogChannel(Snowflake guildId){
        return getTextChannelById(discordEntityRetrieveService.logChannelId(guildId));
    }

    @Override
    public Mono<VoiceChannel> getVoiceChannelById(Snowflake channelId){
        return gateway.getChannelById(channelId).cast(VoiceChannel.class);
    }

    @Override
    public boolean exists(Snowflake userId){
        return gateway.getUserById(userId).map(Objects::nonNull).blockOptional().orElse(false);
    }

    @Override
    public boolean exists(Snowflake guildId, Snowflake userId){
        return gateway.getMemberById(guildId, userId).map(Objects::nonNull).blockOptional().orElse(false);
    }
}
