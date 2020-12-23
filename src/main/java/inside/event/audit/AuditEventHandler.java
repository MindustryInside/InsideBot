package inside.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.*;
import discord4j.discordjson.json.MessageData;
import inside.common.services.*;
import inside.data.service.*;
import inside.util.StringInputStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

public abstract class AuditEventHandler extends ReactiveEventAdapter{
    @Autowired
    protected ContextService context;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected DiscordEntityRetrieveService discordEntityRetrieveService;

    protected StringInputStream stringInputStream = new StringInputStream();

    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        return discordService.getLogChannel(guildId)
                             .publishOn(Schedulers.boundedElastic())
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then(Mono.fromRunnable(() -> context.reset()));
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed){
        return log(guildId, embed, false);
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed, boolean file){
        if(discordEntityRetrieveService.auditDisabled(guildId)) return Mono.empty();
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        return log(guildId, file ? m.addFile("message.txt", stringInputStream) : m);
    }

    public String timestamp(){
        return DateTimeFormat.longDateTime()
                             .withLocale(context.locale())
                             .withZone(context.zone())
                             .print(DateTime.now());
    }

    //todo
}
