package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.*;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.*;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import inside.Settings;
import inside.data.service.*;
import inside.event.dispatcher.EventListener;
import inside.event.dispatcher.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;

import javax.annotation.PreDestroy;
import java.util.*;

@Service
public class DiscordServiceImpl implements DiscordService{
    @Autowired
    private Settings settings;

    @Autowired
    private EntityRetriever retriever;

    @Autowired
    private AdminService adminService;

    protected GatewayDiscordClient gateway;

    protected EventListener eventListener;

    @Autowired(required = false)
    public void init(List<ReactiveEventAdapter> handlers, List<Events> events){
        String token = settings.token;
        Objects.requireNonNull(token, "token");

        gateway = DiscordClientBuilder.create(token)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
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

        Flux.fromIterable(events).subscribe(e -> eventListener.on(e).subscribe());

        Flux.fromIterable(handlers).subscribe(e -> gateway.on(e).subscribe());
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
        eventListener.shutdown();
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
        return gateway.getChannelById(channelId).ofType(TextChannel.class);
    }

    @Override
    public Mono<TextChannel> getLogChannel(Snowflake guildId){
        return getTextChannelById(retriever.logChannelId(guildId));
    }

    @Override
    public Mono<VoiceChannel> getVoiceChannelById(Snowflake channelId){
        return gateway.getChannelById(channelId).ofType(VoiceChannel.class);
    }

    @Override
    public boolean exists(Snowflake userId){
        return gateway.getUserById(userId).hasElement().blockOptional().orElse(false);
    }

    @Override
    public boolean exists(Snowflake guildId, Snowflake userId){
        return gateway.getMemberById(guildId, userId).hasElement().blockOptional().orElse(false);
    }

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    public void activeUsers(){
        Flux.fromIterable(retriever.getAllMembers())
                .filter(localMember -> !retriever.activeUserDisabled(localMember.guildId()) && exists(localMember.guildId(), localMember.userId()))
                .flatMap(localMember -> Mono.zip(Mono.just(localMember), gateway.getMemberById(localMember.guildId(), localMember.userId())))
                .flatMap(TupleUtils.function((localMember, member) -> {
                    Snowflake roleId = retriever.activeUserRoleId(member.getGuildId());
                    if(localMember.isActiveUser()){
                        return member.addRole(roleId);
                    }else{
                        return member.removeRole(roleId).then(Mono.fromRunnable(() -> {
                            localMember.messageSeq(0);
                            retriever.save(localMember);
                        }));
                    }
                }))
                .subscribe();
    }
}
