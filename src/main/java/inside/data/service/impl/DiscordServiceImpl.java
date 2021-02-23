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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import javax.annotation.PreDestroy;
import java.util.*;

@Service
public class DiscordServiceImpl implements DiscordService{
    private final Settings settings;

    private final EntityRetriever retriever;

    private GatewayDiscordClient gateway;

    public DiscordServiceImpl(@Autowired Settings settings,
                              @Autowired EntityRetriever retriever){
        this.settings = settings;
        this.retriever = retriever;
    }

    @Autowired(required = false)
    public void init(List<ReactiveEventAdapter> handlers){
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

        Flux.fromIterable(handlers).subscribe(e -> gateway.on(e).subscribe());
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
    }

    @Override // for monitors
    public GatewayDiscordClient gateway(){
        return gateway;
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
        return Mono.justOrEmpty(retriever.logChannelId(guildId)).flatMap(this::getTextChannelById);
    }

    /* Legacy feature */
    // @Deprecated
    // @Scheduled(cron = "0 */4 * * * *")
    // public void activeUsersMonitor(){
        // Flux.fromIterable(retriever.getAllMembers())
        //         .filter(localMember -> !retriever.activeUserDisabled(localMember.guildId()) && existsMemberById(localMember.guildId(), localMember.userId()))
        //         .flatMap(localMember -> Mono.zip(Mono.just(localMember), gateway.getMemberById(localMember.guildId(), localMember.userId())))
        //         .flatMap(TupleUtils.function((localMember, member) -> {
        //             Snowflake roleId = retriever.activeUserRoleId(member.getGuildId()).orElseThrow(IllegalStateException::new);
        //             if(localMember.isActiveUser()){
        //                 return member.addRole(roleId);
        //             }else{
        //                 localMember.messageSeq(0);
        //                 retriever.save(localMember);
        //                 return member.removeRole(roleId);
        //             }
        //         }))
        //         .subscribe();
    // }
}
