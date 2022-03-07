package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.Configuration;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionCommandHolder;
import inside.service.InteractionService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class InteractionEventHandler extends ReactiveEventAdapter {

    private final Configuration configuration;
    private final InteractionCommandHolder interactionCommandHolder;
    private final InteractionService interactionService;

    public InteractionEventHandler(Configuration configuration, InteractionCommandHolder interactionCommandHolder,
                                   InteractionService interactionService) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.interactionCommandHolder = Objects.requireNonNull(interactionCommandHolder, "interactionCommandHolder");
        this.interactionService = Objects.requireNonNull(interactionService, "interactionService");
    }

    @Override
    public Publisher<?> onSelectMenuInteraction(SelectMenuInteractionEvent event) {
        return interactionService.handleSelectMenuInteractionEvent(event);
    }

    @Override
    public Publisher<?> onButtonInteraction(ButtonInteractionEvent event) {
        return interactionService.handleButtonInteractionEvent(event);
    }

    @Override
    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(interactionCommandHolder.getCommand(event.getCommandName()))
                .flatMap(command -> Mono.from(command.execute(
                        ChatInputInteractionEnvironment.of(configuration, interactionService, event))));
    }
}
