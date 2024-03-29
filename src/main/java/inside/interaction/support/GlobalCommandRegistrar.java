package inside.interaction.support;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.*;
import discord4j.rest.RestClient;
import reactor.core.publisher.Mono;
import reactor.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalCommandRegistrar{

    private static final Logger log = Loggers.getLogger(GlobalCommandRegistrar.class);

    private final RestClient restClient;
    private final List<? extends ApplicationCommandRequest> commandRequests;
    private final Mono<Long> applicationId;

    private GlobalCommandRegistrar(RestClient restClient, List<? extends ApplicationCommandRequest> commandRequests){
        this.restClient = restClient;
        this.commandRequests = commandRequests;
        this.applicationId = restClient.getApplicationId().cache();
    }

    public static GlobalCommandRegistrar create(RestClient restClient, List<? extends ApplicationCommandRequest> commandRequests){
        return new GlobalCommandRegistrar(restClient, commandRequests);
    }

    public Mono<Void> registerCommands(){
        return getExistingCommands()
                .flatMap(existing -> {
                    AtomicInteger createdCount = new AtomicInteger();
                    AtomicInteger updatedCount = new AtomicInteger();
                    AtomicInteger deletedCount = new AtomicInteger();

                    List<Mono<?>> actions = new ArrayList<>();

                    Map<String, ApplicationCommandRequest> commands = new HashMap<>();
                    for(var request : commandRequests){
                        commands.put(request.name(), request);

                        if(!existing.containsKey(request.name())){
                            actions.add(createCommand(request, createdCount));
                        }
                    }

                    for(var existingCommand : existing.values()){
                        long existingCommandId = Snowflake.asLong(existingCommand.id());
                        if(commands.containsKey(existingCommand.name())){
                            var command = commands.get(existingCommand.name());
                            if(RegistrarUtil.isChanged(existingCommand, command)){
                                actions.add(modifyCommand(existingCommandId, command, updatedCount));
                            }
                        }else{
                            actions.add(deleteCommand(existingCommandId, deletedCount));
                        }
                    }

                    return Mono.when(actions)
                            .doFinally(signal -> log.info("Completed global command registering." +
                                    " Created: {}, updated: {}, deleted: {}",
                                    createdCount, updatedCount, deletedCount));
                });
    }

    private Mono<ApplicationCommandData> createCommand(ApplicationCommandRequest request, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .createGlobalApplicationCommand(id, request)
                .doOnNext(it -> counter.incrementAndGet()));
    }

    private Mono<ApplicationCommandData> modifyCommand(long commandId, ApplicationCommandRequest request, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .modifyGlobalApplicationCommand(id, commandId, request)
                .doOnNext(it -> counter.incrementAndGet()));
    }

    private Mono<Void> deleteCommand(long commandId, AtomicInteger counter){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .deleteGlobalApplicationCommand(id, commandId)
                .doFinally(it -> counter.incrementAndGet()));
    }

    private Mono<Map<String, ApplicationCommandData>> getExistingCommands(){
        return applicationId.flatMap(id -> restClient.getApplicationService()
                .getGlobalApplicationCommands(id)
                .collectMap(ApplicationCommandData::name));
    }
}
