package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.Configuration;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionCommandHolder;
import inside.service.InteractionService;
import inside.util.ContextUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;

public class InteractionEventHandler extends ReactiveEventAdapter {

    private final Configuration configuration;
    private final InteractionCommandHolder interactionCommandHolder;
    private final InteractionService interactionService;
    private final EntityRetriever entityRetriever;

    public InteractionEventHandler(Configuration configuration, InteractionCommandHolder interactionCommandHolder,
                                   InteractionService interactionService, EntityRetriever entityRetriever) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.interactionCommandHolder = Objects.requireNonNull(interactionCommandHolder, "interactionCommandHolder");
        this.interactionService = Objects.requireNonNull(interactionService, "interactionService");
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
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
        return Mono.justOrEmpty(event.getInteraction().getGuildId())
                .flatMap(id -> entityRetriever.getGuildConfigById(id)
                        .switchIfEmpty(entityRetriever.createGuildConfig(id)))
                .map(config -> Context.of(ContextUtil.KEY_LOCALE, config.locale(),
                        ContextUtil.KEY_TIMEZONE, config.timezone()))
                .switchIfEmpty(Mono.fromSupplier(() -> Context.of(ContextUtil.KEY_LOCALE, configuration.discord().locale(),
                        ContextUtil.KEY_TIMEZONE, configuration.discord().timezone())))
                .map(ctx -> ChatInputInteractionEnvironment.of(configuration, interactionService, ctx, event))
                .flatMap(env -> Mono.justOrEmpty(interactionCommandHolder.getCommand(event.getCommandName()))
                        .flatMap(command -> Mono.from(command.execute(env)))
                        .contextWrite(env.context()));
    }
}
