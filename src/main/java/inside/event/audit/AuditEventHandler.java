package inside.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.spec.*;
import inside.data.service.*;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.function.Consumer;

import static inside.util.ContextUtil.*;

public abstract class AuditEventHandler extends ReactiveEventAdapter{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected EntityRetriever entityRetriever;

    protected Context context;

    protected StringInputStream stringInputStream = new StringInputStream();

    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        return discordService.getLogChannel(guildId)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                .contextWrite(ContextUtil.reset())
                .then();
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed){
        return log(guildId, embed, false);
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed, boolean file){
        if(entityRetriever.auditDisabled(guildId)){
            return Mono.empty();
        }

        MessageCreateSpec spec = new MessageCreateSpec().setEmbed(embed);
        return log(guildId, file ? spec.addFile("message.txt", stringInputStream) : spec);
    }

    public String timestamp(){
        return DateTimeFormat.longDateTime()
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE))
                .print(DateTime.now());
    }
}
