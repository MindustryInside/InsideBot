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

import java.util.*;
import java.util.regex.Pattern;

public class MessageService extends BaseService {

    public static final ReactionEmoji ok = ReactionEmoji.unicode("✅"), failed = ReactionEmoji.unicode("❌");
    public static final List<Locale> supportedLocaled = List.of(new Locale("ru"), new Locale("en"));
    public static final Map<Locale, Map<String, Pattern>> pluralRules;

    static {
        pluralRules = Map.of(
                supportedLocaled.get(0), Map.of(
                        "zero", Pattern.compile("^\\d*0$"),
                        "one", Pattern.compile("^(-?\\d*[^1])?1$"),
                        "two", Pattern.compile("^(-?\\d*[^1])?2$"),
                        "few", Pattern.compile("(^(-?\\d*[^1])?3)|(^(-?\\d*[^1])?4)$"),
                        "many", Pattern.compile("^\\d+$")
                ),
                supportedLocaled.get(1), Map.of(
                        "zero", Pattern.compile("^0$"),
                        "one", Pattern.compile("^1$"),
                        "other", Pattern.compile("^\\d+$")
                )
        );
    }

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

    public String getPluralized(ContextView ctx, String key, long count){
        String code = key + '.' + getCount0(ctx.get(ContextUtil.KEY_LOCALE), count);
        return get(ctx, code);
    }

    private String getCount0(Locale locale, long value){
        String str = String.valueOf(value);
        var rules = pluralRules.getOrDefault(locale,
                pluralRules.get(configuration.discord().locale()));

        return rules.entrySet().stream()
                .filter(plural -> plural.getValue().matcher(str).find())
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("other");
    }
}
