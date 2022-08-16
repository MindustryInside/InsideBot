package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.service.MessageService;
import inside.util.ResourceMessageSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class InteractionCommand {

    protected final CommandMetadata metadata = CommandMetadata.from(getClass());
    protected final MessageService messageService;

    private final List<ApplicationCommandOptionData> options = new ArrayList<>();

    protected InteractionCommand(MessageService messageService) {
        this.messageService = messageService;
    }

    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        return Mono.empty();
    }

    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {
        return Mono.just(true);
    }

    public String getName() {
        return metadata.name;
    }

    public ApplicationCommandOption.Type getType() {
        return metadata.type;
    }

    public EnumSet<PermissionCategory> getPermissions() {
        return metadata.permissions;
    }

    public ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name(messageService.get(metadata.name + ".name"))
                .nameLocalizationsOrNull(getAll(metadata.name + ".name"))
                .description(messageService.get(metadata.name + ".description"))
                .descriptionLocalizationsOrNull(getAll(metadata.name + ".description"))
                .options(getOptions())
                .build();
    }

    protected Map<String, String> getAll(String code) {
        return ResourceMessageSource.supportedLocaled.stream()
                .filter(l -> !l.equals(messageService.configuration.discord().locale()) &&
                        messageService.messageSource.contains(code, l))
                .collect(Collectors.toMap(Locale::toString, l -> messageService.messageSource.get(code, l)));
    }

    public List<ApplicationCommandOptionData> getOptions() {
        return options;
    }

    @Deprecated
    protected void addOption(Consumer<? super ImmutableApplicationCommandOptionData.Builder> option) {
        var mutatedOption = ApplicationCommandOptionData.builder();
        option.accept(mutatedOption);
        options.add(mutatedOption.build());
    }

    protected void addOption(String name, Consumer<? super ImmutableApplicationCommandOptionData.Builder> option) {
        String base = metadata.name + ".options." + name;
        var mutatedOption = ApplicationCommandOptionData.builder()
                .name(messageService.get(base + ".name"))
                .nameLocalizationsOrNull(getAll(base + ".name"))
                .description(messageService.get(base + ".description"))
                .descriptionLocalizationsOrNull(getAll(base + ".description"));
        option.accept(mutatedOption);
        options.add(mutatedOption.build());
    }

    public record CommandMetadata(String name, ApplicationCommandOption.Type type,
                                  EnumSet<PermissionCategory> permissions) {

        private static CommandMetadata from(Class<? extends InteractionCommand> type) {
            var chatInputCommand = type.getDeclaredAnnotation(ChatInputCommand.class);
            if (chatInputCommand != null) {
                return new CommandMetadata(chatInputCommand.value(),
                        ApplicationCommandOption.Type.UNKNOWN,
                        EnumSet.copyOf(Arrays.asList(chatInputCommand.permissions())));
            }

            var subcommand = type.getDeclaredAnnotation(Subcommand.class);
            if (subcommand != null) {
                return new CommandMetadata(subcommand.value(),
                        ApplicationCommandOption.Type.SUB_COMMAND,
                        EnumSet.noneOf(PermissionCategory.class));
            }

            var subcommandGroup = type.getDeclaredAnnotation(SubcommandGroup.class);
            if (subcommandGroup != null) {
                return new CommandMetadata(subcommandGroup.value(),
                        ApplicationCommandOption.Type.SUB_COMMAND_GROUP,
                        EnumSet.noneOf(PermissionCategory.class));
            }

            throw new UnsupportedOperationException("Unknown annotation");
        }
    }
}
