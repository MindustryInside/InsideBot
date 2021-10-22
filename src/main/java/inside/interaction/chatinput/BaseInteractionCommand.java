package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.*;
import inside.data.service.EntityRetriever;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.service.MessageService;
import inside.util.Preconditions;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

public abstract class BaseInteractionCommand implements InteractionChatInputCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    private final CommandMetadata metadata = CommandMetadata.of(getClass());
    private final List<ApplicationCommandOptionData> options = new ArrayList<>();

    @Override
    public Publisher<Boolean> filter(CommandEnvironment env){
        return Mono.just(true);
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env){
        return Mono.empty();
    }

    @Override
    public String getName(){
        return metadata.name;
    }

    @Override
    public String getDescription(){
        return metadata.description;
    }

    @Override
    public ApplicationCommandOption.Type getType(){
        return metadata.type;
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions(){
        return options;
    }

    @Override
    public ApplicationCommandRequest getRequest(){
        Preconditions.requireState(getType() == ApplicationCommandOption.Type.UNKNOWN,
                "Subcommands mustn't define command request.");

        return ApplicationCommandRequest.builder()
                .name(getName())
                .description(getDescription())
                .options(getOptions())
                .build();
    }

    protected ApplicationCommandOptionData addOption(Consumer<? super ImmutableApplicationCommandOptionData.Builder> option){
        var mutatedOption = ApplicationCommandOptionData.builder();
        option.accept(mutatedOption);
        var build = mutatedOption.build();
        options.add(build);
        return build;
    }

    private static class CommandMetadata{
        private final String name;
        private final String description;
        private final ApplicationCommandOption.Type type;

        private CommandMetadata(String name, String description, ApplicationCommandOption.Type type){
            this.name = name;
            this.description = description;
            this.type = type;
        }

        static CommandMetadata of(Class<? extends InteractionCommand> type){

            var chatInput = type.getAnnotation(ChatInputCommand.class);
            if(chatInput != null){
                return new CommandMetadata(chatInput.name(), chatInput.description(),
                        ApplicationCommandOption.Type.UNKNOWN);
            }

            var subcommand = type.getAnnotation(Subcommand.class);
            if(subcommand != null){
                return new CommandMetadata(subcommand.name(), subcommand.description(),
                        ApplicationCommandOption.Type.SUB_COMMAND);
            }

            var subcommandGroup = type.getAnnotation(SubcommandGroup.class);
            if(subcommandGroup != null){
                return new CommandMetadata(subcommandGroup.name(), subcommandGroup.description(),
                        ApplicationCommandOption.Type.SUB_COMMAND_GROUP);
            }

            throw new UnsupportedOperationException("Unknown type");
        }
    }
}
