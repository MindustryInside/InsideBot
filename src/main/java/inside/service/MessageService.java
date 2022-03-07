package inside.service;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import inside.Configuration;
import inside.interaction.InteractionEnvironment;
import inside.util.ContextUtil;
import inside.util.ResourceMessageSource;
import reactor.util.context.ContextView;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;

public class MessageService extends BaseService {

    public static final ReactionEmoji ok = ReactionEmoji.unicode("✅"), failed = ReactionEmoji.unicode("❌");

    private final Configuration configuration;
    private final ResourceMessageSource messageSource;

    public MessageService(GatewayDiscordClient client, Configuration configuration, ResourceMessageSource messageSource) {
        super(client);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
    }

    public InteractionApplicationCommandCallbackReplyMono infoTitled(InteractionEnvironment env, String title,
                                                                     String text, Object... args) {
        return env.event().reply().withEmbeds(EmbedCreateSpec.builder()
                .title(get(env.context(), title))
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

    public String get(ContextView ctx, String key) {
        try {
            return messageSource.get(key, ctx.get(ContextUtil.KEY_LOCALE));
        } catch (MissingResourceException t) {
            // Лучше уж упадём полностью
            return messageSource.get(key, configuration.discord().locale());
        }
    }

    public String getBool(ContextView ctx, String key, boolean state) {
        return get(ctx, key + (state ? ".on" : ".off"));
    }

    public String getEnum(ContextView ctx, Enum<?> cnts) {
        String key = cnts.getClass().getCanonicalName() + '.' + cnts.name();
        return get(ctx, key.toLowerCase(Locale.ROOT));
    }

    public String format(ContextView ctx, String key, Object... args) {
        try {
            return messageSource.format(key, ctx.get(ContextUtil.KEY_LOCALE), args);
        } catch (MissingResourceException t) {
            try {
                return messageSource.format(key, configuration.discord().locale(), args);
            } catch (MissingResourceException t1) {
                return key;
            }
        }
    }
}
