package inside.interaction.chatinput.settings;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import inside.interaction.CommandEnvironment;
import inside.interaction.chatinput.*;
import org.reactivestreams.Publisher;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SubGroupOwnerCommand<T extends InteractionOwnerCommand>
        extends OwnerAwareCommand<T>
        implements InteractionOwnerCommand{

    protected final Map<String, InteractionChatInputCommand> subcommands = new LinkedHashMap<>();

    protected SubGroupOwnerCommand(T owner, List<? extends InteractionOwnerAwareCommand<?>> subcommands){
        super(owner);

        subcommands.forEach(this::addSubCommand);
    }

    @Override
    public void addSubCommand(InteractionChatInputCommand subcommand){
        subcommands.put(subcommand.getName(), subcommand);
    }

    @Override
    public Optional<InteractionChatInputCommand> getSubCommand(String name){
        return Optional.ofNullable(subcommands.get(name));
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env){
        String commandName = env.event().getOptions().get(0).getOptions().get(0).getName();
        return Mono.justOrEmpty(getSubCommand(commandName)).flatMap(subcmd -> Mono.from(subcmd.execute(env)));
    }

    @Override
    public Publisher<Boolean> filter(CommandEnvironment env){
        String commandName = env.event().getOptions().get(0).getOptions().get(0).getName();
        Mono<Boolean> isSubCommandFilter = Mono.justOrEmpty(getSubCommand(commandName))
                .flatMap(subcmd -> Mono.from(subcmd.filter(env)));

        return BooleanUtils.and(Mono.from(super.filter(env)), isSubCommandFilter);
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
    public Map<String, InteractionChatInputCommand> getSubCommands(){
        return Collections.unmodifiableMap(subcommands);
    }
}
