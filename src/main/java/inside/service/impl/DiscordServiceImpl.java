package inside.service.impl;

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
import inside.interaction.*;
import inside.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.*;
import java.util.*;

@Service
public class DiscordServiceImpl implements DiscordService{

    private GatewayDiscordClient gateway;

    @Autowired(required = false)
    private List<InteractionCommand> commands;

    @Autowired(required = false)
    private ReactiveEventAdapter[] adapters;

    @Autowired
    private Settings settings;

    @PostConstruct
    public void init(){
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
                        Intent.DIRECT_MESSAGES
                ))
                .login()
                .block();

        Objects.requireNonNull(gateway, "impossible"); // for ide

        long applicationId = Objects.requireNonNull(gateway.rest().getApplicationId().block(), "impossible");
        for(InteractionCommand command : commands){
            gateway.rest().getApplicationService()
                    .createGlobalApplicationCommand(applicationId, command.getRequest())
                    .subscribe();
        }

        gateway.on(ReactiveEventAdapter.from(adapters)).subscribe();
    }

    @Override
    public Mono<Void> handle(InteractionCommandEnvironment env){
        return Mono.justOrEmpty(commands.stream()
                .filter(cmd -> cmd.getRequest().name().equals(env.event().getCommandName()))
                .findFirst())
                .filterWhen(cmd -> cmd.apply(env))
                .flatMap(cmd -> cmd.execute(env));
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
    }

    @Override
    public List<InteractionCommand> getCommands(){
        return commands;
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
