package inside.event.audit;

import discord4j.core.spec.*;
import inside.data.entity.*;
import inside.data.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.util.List;

public abstract class BaseAuditForwardProvider implements AuditForwardProvider{

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Override
    public Mono<Void> send(GuildConfig config, AuditAction action, List<Tuple2<String, InputStream>> attachments){
        return Mono.justOrEmpty(config.logChannelId()).flatMap(discordService::getTextChannelById)
                .flatMap(textChannel -> Mono.deferContextual(ctx -> textChannel.createMessage(spec -> {
                    spec.setEmbed(embed -> build(action, ctx, spec, embed.setColor(action.type().color)));
                    attachments.forEach(TupleUtils.consumer(spec::addFile));
                })))
                .then();
    }

    protected abstract void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed);
}
