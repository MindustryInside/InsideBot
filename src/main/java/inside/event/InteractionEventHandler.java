package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.InteractionCreateEvent;
import inside.Settings;
import inside.data.service.EntityRetriever;
import inside.interaction.InteractionCommandEnvironment;
import inside.service.DiscordService;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.context.Context;

import java.util.Optional;

@Component
public class InteractionEventHandler extends ReactiveEventAdapter{

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private Settings settings;

    @Autowired
    private DiscordService discordService;

    @Override
    public Publisher<?> onInteractionCreate(InteractionCreateEvent event){
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        Context context = Context.of(ContextUtil.KEY_LOCALE, guildId.map(entityRetriever::getLocale)
                        .orElse(LocaleUtil.getDefaultLocale()),
                ContextUtil.KEY_TIMEZONE, guildId.map(entityRetriever::getTimeZone)
                        .orElse(settings.getDefaults().getTimeZone()));

        InteractionCommandEnvironment environment = InteractionCommandEnvironment.builder()
                .event(event)
                .context(context)
                .build();
        return discordService.handle(environment).contextWrite(context);
    }
}
