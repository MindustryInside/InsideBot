package inside.interaction.chatinput.settings;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class ConfigOwnerCommand extends InteractionConfigCommand {

    protected final Map<String, InteractionCommand> subcommands = new LinkedHashMap<>();

    public ConfigOwnerCommand(MessageService messageService, EntityRetriever entityRetriever){
        super(messageService, entityRetriever);
    }

    protected void addSubcommand(InteractionCommand subcommand){
        subcommands.put(subcommand.getName(), subcommand);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment event){
        var command = subcommands.keySet().stream()
                .map(event::getOption)
                .flatMap(Optional::stream)
                .findFirst();

        return Mono.justOrEmpty(command).flatMapMany(opt -> subcommands.get(opt.getName()).execute(event));
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
}
