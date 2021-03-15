package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.*;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import inside.Settings;
import inside.data.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.util.Objects;

@Service
public class DiscordServiceImpl implements DiscordService{
    private final Settings settings;

    private GatewayDiscordClient gateway;

    public DiscordServiceImpl(@Autowired Settings settings){
        this.settings = settings;
    }

    @Autowired(required = false)
    public void init(ReactiveEventAdapter[] handlers){
        String token = settings.getToken();
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

        Objects.requireNonNull(gateway); // for ide

        gateway.on(ReactiveEventAdapter.from(handlers)).subscribe();
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
    public Mono<TextChannel> getTextChannelById(Snowflake channelId){
        return gateway.getChannelById(channelId).ofType(TextChannel.class);
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
