package insidebot.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.*;
import discord4j.discordjson.json.MessageData;
import insidebot.common.services.*;
import insidebot.data.service.*;
import insidebot.util.StringInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public abstract class AuditEventHandler extends ReactiveEventAdapter{
    @Autowired
    protected ContextService context;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected GuildService guildService;

    protected StringInputStream stringInputStream = new StringInputStream();

    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        MessageData data = discordService.getLogChannel(guildId)
                                         .map(TextChannel::getRestChannel)
                                         .flatMap(c -> c.createMessage(message.asRequest()))
                                         .block();
        return Mono.justOrEmpty(data).flatMap(__ -> Mono.fromRunnable(() -> context.reset()));
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed){
        return log(guildId, embed, false);
    }

    public Mono<Void> log(Snowflake guildId, Consumer<EmbedCreateSpec> embed, boolean file){
        if(guildService.auditDisabled(guildId)) return Mono.empty();
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        return log(guildId, file ? m.addFile("message.txt", stringInputStream) : m);
    }

    //todo
}
