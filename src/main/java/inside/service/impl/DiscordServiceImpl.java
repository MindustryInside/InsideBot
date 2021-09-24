package inside.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import discord4j.common.*;
import discord4j.common.util.Snowflake;
import discord4j.core.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.possible.PossibleModule;
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
import inside.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.*;

import javax.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.*;

@Service
public class DiscordServiceImpl implements DiscordService{

    private static final Logger log = Loggers.getLogger("inside.service.ActiveUserMonitor");
    private final Map<String, InteractionCommand> commandMap = new LinkedHashMap<>();

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
                .setDefaultAllowedMentions(AllowedMentions.builder()
                        .parseType(AllowedMentions.Type.USER)
                        .build())
                .setJacksonResources(JacksonResources.createFromObjectMapper(new ObjectMapper())
                        .withMapperFunction(mapper -> mapper.registerModule(new JavaTimeModule())
                                .registerModule(new ParameterNamesModule())
                                .registerModule(new PossibleModule())))
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
        gateway.rest().getApplicationService()
                .bulkOverwriteGlobalApplicationCommand(applicationId, commands.stream()
                        .filter(cmd -> cmd.getType() == ApplicationCommandOption.Type.UNKNOWN)
                        .map(cmd -> {
                            var req = cmd.getRequest();
                            commandMap.put(req.name(), cmd);
                            return req;
                        })
                        .collect(Collectors.toList()))
                .subscribe();

        gateway.on(ReactiveEventAdapter.from(adapters)).subscribe();
    }

    @Override
    public Mono<Void> handle(InteractionCommandEnvironment env){
        return Mono.justOrEmpty(commandMap.get(env.event().getCommandName()))
                .filterWhen(cmd -> cmd.filter(env))
                .flatMap(cmd -> cmd.execute(env));
    }

    @PreDestroy
    public void destroy(){
        gateway.logout().block();
    }

    @Override
    public List<? extends InteractionCommand> getCommands(){
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
                    if(activeUserConfig.isActive(activity)){
                        return member.addRole(roleId);
                    }
                    return member.removeRole(roleId);
                }).and(Mono.defer(() -> {
                    if(activeUserConfig.resetIfAfter(localMember.getActivity())){
                        return entityRetriever.save(localMember);
                    }
                    return Mono.empty();
                }))))
                .onErrorResume(ClientException.class, t -> {
                    DiscordWebRequest req = t.getRequest().getDiscordRequest();
                    String major = RouteUtils.getMajorParam(req.getRoute().getUriTemplate(), req.getCompleteUri());
                    if(major == null){ // the exception will always start with 'guilds/{guild.id}'
                        return Mono.error(t);
                    }

                    return Mono.deferContextual(ctx -> Mono.fromRunnable(() -> log.error(LogUtil.format(ctx, "Missing access"))))
                            .contextWrite(ctx1 -> ctx1.put(LogUtil.KEY_GUILD_ID, major))
                            .then(Mono.empty());
                })
                .subscribe();
    }
}
