package inside.service;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import inside.Configuration;

import java.text.MessageFormat;
import java.util.Objects;

public class MessageService extends BaseService {

    private final Configuration configuration;

    public MessageService(GatewayDiscordClient client, Configuration configuration) {
        super(client);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public InteractionApplicationCommandCallbackReplyMono info(DeferrableInteractionEvent event, String title, String text, Object... args) {
        return event.reply().withEmbeds(EmbedCreateSpec.builder()
                .title(title)
                .description(MessageFormat.format(text, args))
                .color(configuration.discord().embedColor())
                .build());
    }

    public InteractionApplicationCommandCallbackReplyMono err(DeferrableInteractionEvent event, String text, Object... values) {
        return event.reply()
                .withEphemeral(true)
                .withEmbeds(EmbedCreateSpec.builder()
                        .color(configuration.discord().embedErrorColor())
                        .description(MessageFormat.format(text, values))
                        .build());
    }
}
