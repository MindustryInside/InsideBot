package inside.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import discord4j.common.*;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.*;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.*;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.rest.util.*;
import inside.Settings;
import inside.data.entity.Activity;
import inside.data.service.EntityRetriever;
import inside.interaction.*;
import inside.interaction.chatinput.InteractionChatInputCommand;
import inside.interaction.chatinput.common.GuildCommand;
import inside.interaction.user.UserCommand;
import inside.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.*;

@Service
public class DiscordServiceImpl implements DiscordService{

    private final Map<String, InteractionChatInputCommand> chatInputCommandMap = new LinkedHashMap<>();
    private final Map<String, UserCommand> userCommandMap = new LinkedHashMap<>();

    private GatewayDiscordClient gateway;

    @Autowired(required = false)
    private List<InteractionCommand> commands;

    @Autowired(required = false)
    private ReactiveEventAdapter[] adapters;

    @Autowired
    private Settings settings;

    @Autowired
    private EntityRetriever entityRetriever;

    @PostConstruct
    public void init(){
        String token = settings.getToken();
        Objects.requireNonNull(token, "token");

        gateway = DiscordClientBuilder.create(token)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400))
                .setDefaultAllowedMentions(AllowedMentions.suppressAll())
                .setJacksonResources(JacksonResources.createFromObjectMapper(new ObjectMapper())
                        .withMapperFunction(mapper -> mapper.registerModule(new JavaTimeModule())
                                .registerModule(new ParameterNamesModule())))
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
                        Intent.GUILD_WEBHOOKS
                ))
                .login()
                .blockOptional()
                .orElseThrow(IllegalStateException::new);

        long applicationId = gateway.rest().getApplicationId().blockOptional().orElse(0L);

        List<ApplicationCommandRequest> guildCommands = new ArrayList<>();
        var commands0 = commands.stream()
                .filter(cmd -> cmd.getType() == ApplicationCommandOption.Type.UNKNOWN)
                .map(cmd -> {
                    var req = cmd.getRequest();

                    String name = req.name();
                    switch(cmd.getCommandType()){
                        case USER -> userCommandMap.put(name, (UserCommand)cmd);
                        case CHAT_INPUT -> chatInputCommandMap.put(name, (InteractionChatInputCommand)cmd);
                    }

                    if(cmd instanceof GuildCommand){
                        guildCommands.add(req);
                        return null;
                    }

                    return req;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        gateway.rest().getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(applicationId, commands0)
                .subscribe();

        gateway.rest().getGuilds()
                .flatMap(data -> gateway.rest().getApplicationService()
                        .bulkOverwriteGuildApplicationCommand(applicationId, data.id().asLong(),
                                guildCommands))
                .subscribe();

        gateway.on(ReactiveEventAdapter.from(adapters)).subscribe();
    }

    @Override
    public Mono<Void> handleChatInputCommand(InteractionCommandEnvironment env){
        return Mono.justOrEmpty(chatInputCommandMap.get(env.event().getCommandName()))
                .filterWhen(cmd -> cmd.filter(env))
                .flatMap(cmd -> cmd.execute(env));
    }

    @Override
    public Mono<Void> handleUserCommand(InteractionUserEnvironment env){
        return Mono.justOrEmpty(userCommandMap.get(env.event().getCommandName()))
                .filterWhen(cmd -> cmd.filter(env))
                .flatMap(cmd -> cmd.execute(env));
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
    }

    @Override // for monitors
    public GatewayDiscordClient gateway(){
        return gateway;
    }

    // TODO: replace to lazy variant
    @Scheduled(cron = "0 */2 * * * *")
    private void activeUsers(){
        entityRetriever.getAllLocalMembers()
                .flatMap(localMember -> Mono.zip(Mono.just(localMember),
                        gateway.getMemberById(localMember.getGuildId(), localMember.getUserId()),
                        entityRetriever.getActivityConfigById(localMember.getGuildId())))
                .filter(predicate((localMember, member, activeUserConfig) -> activeUserConfig.isEnabled()))
                .flatMap(function((localMember, member, activeUserConfig) -> Mono.defer(() -> {
                    Snowflake roleId = activeUserConfig.getRoleId().orElse(null);
                    if(roleId == null){
                        return Mono.empty();
                    }

                    Activity activity = localMember.getActivity();
                    return activeUserConfig.isActive(activity) ? member.addRole(roleId) : member.removeRole(roleId);
                }).and(Mono.defer(() -> activeUserConfig.resetIfAfter(localMember.getActivity())
                        ? entityRetriever.save(localMember)
                        : Mono.empty()))))
                .onErrorResume(ClientException.class, t -> Mono.empty())
                .subscribe();
    }
}
