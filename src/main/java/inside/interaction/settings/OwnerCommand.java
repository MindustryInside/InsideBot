package inside.interaction.settings;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import inside.interaction.*;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

public abstract class OwnerCommand extends SettingsCommand implements InteractionOwnerCommand{

    protected final Map<String, InteractionCommand> subcommands = new LinkedHashMap<>();

    protected OwnerCommand(List<? extends InteractionOwnerAwareCommand<?>> subcommands){

        subcommands.forEach(this::addSubCommand);
    }

    @Override
    public void addSubCommand(InteractionCommand subcommand){
        subcommands.put(subcommand.getName(), subcommand);
    }

    @Override
    public Optional<InteractionCommand> getSubCommand(String name){
        return Optional.ofNullable(subcommands.get(name));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        String commandName = env.event().getOptions().get(0).getName();
        return Mono.justOrEmpty(getSubCommand(commandName)).flatMap(subcmd -> subcmd.execute(env));
    }

    @Override
    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        String commandName = env.event().getOptions().get(0).getName();
        Mono<Boolean> isSubCommandFilter = Mono.justOrEmpty(getSubCommand(commandName))
                .flatMap(subcmd -> subcmd.filter(env));

        return BooleanUtils.and(super.filter(env), isSubCommandFilter);
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions(){
        return subcommands.values().stream()
                .map(subcommand -> ApplicationCommandOptionData.builder()
                        .name(subcommand.getName())
                        .description(subcommand.getDescription())
                        .type(subcommand.getType().getValue())
                        .options(subcommand.getOptions())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, InteractionCommand> getSubCommands(){
        return Collections.unmodifiableMap(subcommands);
    }
}