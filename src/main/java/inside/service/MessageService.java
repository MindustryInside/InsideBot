package inside.service;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.core.spec.MessageCreateMono;
import discord4j.core.spec.MessageCreateSpec;
import inside.Configuration;
import inside.command.CommandEnvironment;
import inside.interaction.InteractionEnvironment;
import inside.util.ContextUtil;
import inside.util.ResourceMessageSource;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.MissingResourceException;
import java.util.Objects;

public class MessageService extends BaseService {

    public static final ReactionEmoji ok = ReactionEmoji.unicode("✅"), failed = ReactionEmoji.unicode("❌");

    public final Configuration configuration;
    public final ResourceMessageSource messageSource;

    public MessageService(GatewayDiscordClient client, Configuration configuration) {
        super(client);
        this.configuration = Objects.requireNonNull(configuration);

        this.messageSource = new ResourceMessageSource("bundle");
    }

    public InteractionApplicationCommandCallbackReplyMono infoTitled(InteractionEnvironment env, String title,
                                                                     String text, Object... args) {
        return env.event().reply().withEmbeds(EmbedCreateSpec.builder()
                .title(title)
                .description(format(env.context(), text, args))
                .color(configuration.discord().embedColor())
                .build());
    }

    public InteractionApplicationCommandCallbackReplyMono info(InteractionEnvironment env, String text, Object... args) {
        return env.event().reply().withEmbeds(EmbedCreateSpec.builder()
                .description(format(env.context(), text, args))
                .color(configuration.discord().embedColor())
                .build());
    }

    public InteractionApplicationCommandCallbackReplyMono err(InteractionEnvironment env, String text, Object... values) {
        return env.event().reply()
                .withEphemeral(true)
                .withEmbeds(EmbedCreateSpec.builder()
                        .color(configuration.discord().embedErrorColor())
                        .description(format(env.context(), text, values))
                        .build());
    }

    public InteractionApplicationCommandCallbackReplyMono text(InteractionEnvironment env, String text, Object... values) {
        return env.event().reply(format(env.context(), text, values));
    }

    // Для текстовых команд

    public MessageCreateMono text(CommandEnvironment env, String text, Object... values) {
        return env.channel().createMessage(format(env.context(), text, values));
    }

    public Mono<Void> err(CommandEnvironment env, String text, Object... values) {
        return env.channel().createMessage(EmbedCreateSpec.builder()
                        .description(format(env.context(), text, values))
                        .color(configuration.discord().embedErrorColor())
                        .build())
                .flatMap(message -> Mono.delay(configuration.discord().embedErrorTtl())
                        .then(message.delete().and(env.message().addReaction(failed))));
    }

    public Mono<Void> errTitled(CommandEnvironment env, String title, String text, Object... args) {
        return env.channel().createMessage(MessageCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .color(configuration.discord().embedErrorColor())
                                .description(format(env.context(), text, args))
                                .title(title)
                                .build())
                        .build())
                .flatMap(message -> Mono.delay(configuration.discord().embedErrorTtl())
                        .then(message.delete().and(env.message().addReaction(failed))));
    }

    public String get(String key) {
        return messageSource.get(key, configuration.discord().locale());
    }

    public String get(ContextView ctx, String key) {
        try {
            return messageSource.get(key, ctx.get(ContextUtil.KEY_LOCALE));
        } catch (MissingResourceException t) {
            // Лучше уж упадём полностью
            return messageSource.get(key, configuration.discord().locale());
        }
    }

    public String format(ContextView ctx, String key, Object... args) {
        try {
            return messageSource.format(key, ctx.get(ContextUtil.KEY_LOCALE), args);
        } catch (MissingResourceException t) {
            return messageSource.format(key, configuration.discord().locale(), args);
        }
    }

    public String getPluralized(ContextView ctx, String key, long count) {
        return messageSource.plural(key, ctx.get(ContextUtil.KEY_LOCALE), count);
    }

    public String getEnum(ContextView ctx, Enum<?> cnts) {
        return get(ctx, cnts.getClass().getCanonicalName() + '.' + cnts.name());
    }
}
