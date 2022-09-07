package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InteractionSubcommandGroup extends InteractionCommand {

    protected final EntityRetriever entityRetriever;

    protected final Map<String, InteractionCommand> subcommands = new HashMap<>();

    public InteractionSubcommandGroup(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = entityRetriever;
    }

    protected void addSubcommand(InteractionCommand command) {
        String nameCode = info.nameCode() + '.' + command.info.nameCode();
        subcommands.put(messageService.get(nameCode), command);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment event) {
        var command = subcommands.keySet().stream()
                .map(event::getOption)
                .flatMap(Optional::stream)
                .findFirst();

        return Mono.justOrEmpty(command).flatMapMany(opt -> subcommands.get(opt.getName()).execute(event));
    }

    @Override
    public final ApplicationCommandRequest asRequest() {
        String baseNameCode = info.nameCode() + '.';
        return ApplicationCommandRequest.builder()
                .name(messageService.get(info.nameCode() + ".name"))
                .nameLocalizationsOrNull(getAll(info.nameCode() + ".name"))
                .description(messageService.get(info.nameCode() + ".description"))
                .descriptionLocalizationsOrNull(getAll(info.nameCode() + ".description"))
                .options(subcommands.entrySet().stream()
                        .map(e -> ApplicationCommandOptionData.builder()
                                .name(e.getKey())
                                .nameLocalizationsOrNull(getAll(baseNameCode + e.getValue().info.nameCode() + ".name"))
                                .description(messageService.get(baseNameCode + e.getValue().info.nameCode() + ".description"))
                                .descriptionLocalizationsOrNull(getAll(baseNameCode + e.getValue().info.nameCode() + ".description"))
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .options(e.getValue().info.options().stream()
                                        .map(this::fromOption)
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
