package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class InteractionCommand {

    protected final CommandMetadata metadata = CommandMetadata.from(getClass());
    protected final MessageService messageService;

    private final List<ApplicationCommandOptionData> options = new ArrayList<>();

    protected InteractionCommand(MessageService messageService) {
        this.messageService = Objects.requireNonNull(messageService, "messageService");
    }

    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        return Mono.empty();
    }

    public String getName() {
        return metadata.name;
    }

    public String getDescription() {
        return metadata.description;
    }

    public ApplicationCommandOption.Type getType() {
        return metadata.type;
    }

    public ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name(metadata.name)
                .description(metadata.description)
                .options(getOptions())
                .build();
    }

    public List<ApplicationCommandOptionData> getOptions() {
        return options;
    }

    protected void addOption(Consumer<? super ImmutableApplicationCommandOptionData.Builder> option) {
        var mutatedOption = ApplicationCommandOptionData.builder();
        option.accept(mutatedOption);
        options.add(mutatedOption.build());
    }

    private record CommandMetadata(String name, String description, ApplicationCommandOption.Type type) {

        private static CommandMetadata from(Class<? extends InteractionCommand> type) {
            var chatInputCommand = type.getDeclaredAnnotation(ChatInputCommand.class);
            if (chatInputCommand != null) {
                return new CommandMetadata(chatInputCommand.name(), chatInputCommand.description(),
                        ApplicationCommandOption.Type.UNKNOWN);
            }

            var subcommand = type.getDeclaredAnnotation(Subcommand.class);
            if (subcommand != null) {
                return new CommandMetadata(subcommand.name(), subcommand.description(),
                        ApplicationCommandOption.Type.SUB_COMMAND);
            }

            var subcommandGroup = type.getDeclaredAnnotation(SubcommandGroup.class);
            if (subcommandGroup != null) {
                return new CommandMetadata(subcommandGroup.name(), subcommandGroup.description(),
                        ApplicationCommandOption.Type.SUB_COMMAND_GROUP);
            }

            throw new UnsupportedOperationException("Unknown annotation");
        }
    }
}
