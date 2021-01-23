package inside.data.service.impl;

import arc.util.Log;
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
import inside.data.repository.LocalMemberRepository;
import inside.data.service.*;
import inside.event.StartupEventHandler;
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
    private DiscordEntityRetrieveService discordEntityRetrieveService;

    @Autowired
    private AdminService adminService;

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
                .withEventDispatcher(dispatcher -> handlers.stream()
                        .filter(adapter -> adapter instanceof StartupEventHandler).findFirst()
                        .map(dispatcher::on)
                        .orElse(Flux.empty())
                )
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

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    public void unmuteUsers(){
        Flux.fromIterable(discordEntityRetrieveService.getAllMembers())
                .filter(m -> !discordEntityRetrieveService.muteDisabled(m.guildId()))
                .filter(this::isMuteEnd)
                .subscribe(l -> {
                    eventListener.publish(new EventType.MemberUnmuteEvent(gateway.getGuildById(l.guildId()).block(), l));
                }, Log::err);
    }

    @Transactional
    @Scheduled(cron = "0 */2 * * * *")
    public void activeUsers(){
        Flux.fromIterable(discordEntityRetrieveService.getAllMembers())
                .filter(m -> !discordEntityRetrieveService.activeUserDisabled(m.guildId()))
                .filterWhen(l -> discordEntityRetrieveService.existsMemberById(l.guildId(), l.userId()) ? Mono.just(true) : Mono.fromRunnable(() -> discordEntityRetrieveService.deleteMember(l)))
                .subscribe(l -> {
                    Member member = gateway.getMemberById(l.guildId(), l.userId()).block();
                    if(member == null) return; // нереально
                    Snowflake roleId = discordEntityRetrieveService.activeUserRoleId(member.getGuildId());
                    if(l.isActiveUser()){
                        member.addRole(roleId).block();
                    }else{
                        member.removeRole(roleId).block();
                        l.messageSeq(0);
                        discordEntityRetrieveService.save(l);
                    }
                }, Log::err);
    }

    protected boolean isMuteEnd(LocalMember member){
        AdminAction action = adminService.get(AdminService.AdminActionType.mute, member.guildId(), member.userId()).blockFirst();
        return action != null && action.isEnd();
    }
}
