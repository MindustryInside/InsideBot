package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.*;
import discord4j.rest.response.ResponseFunction;
import inside.Settings;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.dispatcher.EventListener;
import inside.event.dispatcher.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;

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
        return gateway.getUserById(userId).map(Objects::nonNull).blockOptional().orElse(false);
    }

    @Override
    public boolean exists(Snowflake guildId, Snowflake userId){
        return gateway.getMemberById(guildId, userId).map(Objects::nonNull).blockOptional().orElse(false);
    }

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    public void unmuteUsers(){
        Flux.fromIterable(retriever.getAllMembers())
                .filter(localMember -> !retriever.muteDisabled(localMember.guildId()))
                .filterWhen(this::isMuteEnd)
                .subscribe(localMember -> eventListener.publish(new EventType.MemberUnmuteEvent(gateway.getGuildById(localMember.guildId()).block(), localMember)));
    }

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    public void activeUsers(){
        Flux.fromIterable(retriever.getAllMembers())
                .filter(localMember -> !retriever.activeUserDisabled(localMember.guildId()))
                .subscribe(localMember -> {
                    Member member = gateway.getMemberById(localMember.guildId(), localMember.userId()).block();
                    if(member == null) return; // нереально
                    Snowflake roleId = retriever.activeUserRoleId(member.getGuildId());
                    if(localMember.isActiveUser()){
                        member.addRole(roleId).block();
                    }else{
                        member.removeRole(roleId).block();
                        localMember.messageSeq(0);
                        retriever.save(localMember);
                    }
                });
    }

    protected Mono<Boolean> isMuteEnd(LocalMember member){
        return adminService.get(AdminService.AdminActionType.mute, member.guildId(), member.userId())
                .next()
                .map(AdminAction::isEnd);
    }
}
