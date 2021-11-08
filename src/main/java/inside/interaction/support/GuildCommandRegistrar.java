package inside.interaction.support;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.*;
import discord4j.rest.RestClient;
import reactor.core.publisher.Mono;
import reactor.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GuildCommandRegistrar{

    private static final Logger log = Loggers.getLogger(GuildCommandRegistrar.class);

    private final RestClient restClient;
    private final long guildId;
    private final List<? extends ApplicationCommandRequest> commandRequests;
    private final Mono<Long> applicationId;

    private GuildCommandRegistrar(RestClient restClient, long guildId,
                                  List<? extends ApplicationCommandRequest> commandRequests){
        this.restClient = restClient;
        this.guildId = guildId;
        this.commandRequests = commandRequests;
        this.applicationId = restClient.getApplicationId().cache();
    }

    public static GuildCommandRegistrar create(RestClient restClient, long guildId,
                                               List<? extends ApplicationCommandRequest> commandRequests){
        return new GuildCommandRegistrar(restClient, guildId, commandRequests);
    }

    public Mono<Void> registerCommands(){
        return getExistingCommands()
                .flatMap(existingCommands -> {
                    AtomicInteger createdCount = new AtomicInteger();
                    AtomicInteger updatedCount = new AtomicInteger();
                    AtomicInteger deletedCount = new AtomicInteger();

                    List<Mono<?>> actions = new ArrayList<>();

                    Map<String, ApplicationCommandRequest> commands = new HashMap<>();
                    for(var request : commandRequests){
                        commands.put(request.name(), request);

                        if(!existingCommands.containsKey(request.name())){
                            actions.add(createCommand(request, createdCount));
                        }
                    }

                    for(var existingCommand : existingCommands.values()){
                        long existingCommandId = Snowflake.asLong(existingCommand.id());
                        if(commands.containsKey(existingCommand.name())){
                            var command = commands.get(existingCommand.name());
                            if(isChanged(existingCommand, command)){
                                actions.add(modifyCommand(existingCommandId, command, updatedCount));
                            }
                        }else{
                            actions.add(deleteCommand(existingCommandId, deletedCount));
                        }
                    }

                    return Mono.when(actions)
                            .doFinally(signal -> log.info("Completed guild command registering for guild {}." +
                                    " Created: {}, updated: {}, deleted: {}",
                                    Snowflake.asString(guildId), createdCount, updatedCount, deletedCount));
                });
    }

    private Mono<ApplicationCommandData> createCommand(ApplicationCommandRequest request, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .createGuildApplicationCommand(id, guildId, request)
                .doOnNext(it -> counter.incrementAndGet()));
    }

    private Mono<ApplicationCommandData> modifyCommand(long commandId, ApplicationCommandRequest request, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .modifyGuildApplicationCommand(id, guildId, commandId, request)
                .doOnNext(it -> counter.incrementAndGet()));
    }

    private Mono<Void> deleteCommand(long commandId, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .deleteGuildApplicationCommand(id, guildId, commandId)
                .doFinally(it -> counter.incrementAndGet()));
    }

    private boolean isChanged(ApplicationCommandData oldCommand, ApplicationCommandRequest newCommand){
        return !oldCommand.type().toOptional().orElse(1).equals(newCommand.type().toOptional().orElse(1))
                || !oldCommand.description().equals(newCommand.description().toOptional().orElse(""))
                || oldCommand.defaultPermission().toOptional().orElse(false)
                != newCommand.defaultPermission().toOptional().orElse(false)
                || !oldCommand.options().toOptional().orElse(List.of())
                .equals(newCommand.options().toOptional().orElse(List.of()));
    }

    private Mono<Map<String, ApplicationCommandData>> getExistingCommands(){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .getGuildApplicationCommands(id, guildId)
                .collectMap(ApplicationCommandData::name));
    }
}
