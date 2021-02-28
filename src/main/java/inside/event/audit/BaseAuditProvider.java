package inside.event.audit;

import discord4j.core.spec.*;
import inside.data.entity.*;
import inside.data.service.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.List;

import static inside.util.ContextUtil.*;

public abstract class BaseAuditProvider implements AuditProvider{
    public static final String MESSAGE_TXT = "message.txt";

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Override
    public Mono<Void> send(GuildConfig config, AuditAction action, List<Tuple2<String, InputStream>> attachments){
        return Mono.justOrEmpty(config.logChannelId())
                .flatMap(discordService::getTextChannelById)
                .flatMap(textChannel -> Mono.deferContextual(ctx -> textChannel.createMessage(spec -> {
                    spec.setEmbed(embed -> build(action, ctx, spec, embed.setColor(action.type().color)));
                    attachments.forEach(TupleUtils.consumer(spec::addFile));
                })))
                .then();
    }

    // oh no, why we use DateTime#now, we have an action timestamp(?)
    protected void addTimestamp(ContextView context, EmbedCreateSpec embed){
        embed.setFooter(DateTimeFormat.longDateTime().withLocale(context.get(KEY_LOCALE)).withZone(context.get(KEY_TIMEZONE)).print(DateTime.now()), null);
    }

    protected abstract void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed);
}
