package insidebot.common.services.impl;

import arc.util.Log;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.channel.*;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.*;
import insidebot.Settings;
import insidebot.common.services.DiscordService;
import insidebot.event.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.util.annotation.NonNull;

import javax.annotation.*;
import java.util.*;

@Service
public class DiscordServiceImpl implements DiscordService{
    @Autowired
    private Settings settings;

    @Autowired(required = false)
    private List<EventHandler<? super Event>> handlers;

    protected GatewayDiscordClient gateway;

    @PostConstruct
    public void init(){
        String token = settings.token;
        Objects.requireNonNull(token, "Discord token not provided");

        gateway = DiscordClient.create(token)
                               .gateway()
                               .setMemberRequestFilter(MemberRequestFilter.all())
                               .setEnabledIntents(IntentSet.of(
                                       Intent.GUILD_MEMBERS,
                                       Intent.GUILDS,
                                       Intent.GUILD_MESSAGES,
                                       Intent.GUILD_MESSAGE_REACTIONS,
                                       Intent.DIRECT_MESSAGES,
                                       Intent.DIRECT_MESSAGE_REACTIONS
                               ))
                               .login()
                               .block();

        Flux.fromIterable(handlers)
            .filter(Objects::nonNull)
            .subscribe(e -> {
                gateway.on(e.type())
                       .flatMap(e::onEvent)
                       .subscribe();
            }, Log::err);

        gateway.onDisconnect().block();
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
    public Mono<TextChannel> getTextChannelById(@NonNull Snowflake guildId){
        return gateway.getChannelById(guildId).cast(TextChannel.class);
    }

    @Override
    public Mono<VoiceChannel> getVoiceChannelById(@NonNull Snowflake guildId){
        return gateway.getChannelById(guildId).cast(VoiceChannel.class);
    }
}
