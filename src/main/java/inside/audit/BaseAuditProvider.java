package inside.audit;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import discord4j.rest.util.Permission;
import inside.data.entity.*;
import inside.data.entity.base.NamedReference;
import inside.service.*;
import inside.util.DiscordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;

import java.io.InputStream;
import java.time.format.*;
import java.util.List;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

public abstract class BaseAuditProvider implements AuditProvider{
    public static final String MESSAGE_TXT = "message.txt";

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected DiscordService discordService;

    @Override
    public Mono<Void> send(AuditConfig config, AuditActionBuilder action, List<? extends Tuple2<String, InputStream>> attachments){
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(config.getLogChannelId())
                .flatMap(discordService.gateway()::getChannelById)
                .cast(GuildMessageChannel.class)
                .filter(ignored -> config.isEnabled(action.getType()))
                .filterWhen(channel -> channel.getEffectivePermissions(discordService.gateway().getSelfId())
                        .map(set -> set.contains(Permission.SEND_MESSAGES))
                        .filterWhen(bool -> bool ? Mono.just(true) : discordService.gateway()
                                .getGuildById(action.getGuildId())
                                .flatMap(Guild::getOwner)
                                .flatMap(User::getPrivateChannel)
                                .flatMap(dm -> dm.createMessage(messageService.format(ctx, "audit.permission-denied",
                                        DiscordUtil.getChannelMention(channel.getId()))))
                                .thenReturn(false)))
                .flatMap(channel -> {
                    var embedSpec = EmbedCreateSpec.builder()
                            .color(action.getType().color);

                    build(action, ctx, embedSpec);

                    return channel.createMessage(MessageCreateSpec.builder()
                            .addEmbed(embedSpec.build())
                            .files(attachments.stream()
                                    .map(function(MessageCreateFields.File::of))
                                    .collect(Collectors.toList()))
                            .build());
                })
                .then());
    }

    protected void addTimestamp(ContextView context, AuditActionBuilder action, EmbedCreateSpec.Builder embed){
        embed.footer(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE))
                .format(action.getTimestamp()), null);
    }

    protected String getShortReference(ContextView context, Entity reference){
        return messageService.format(context, "audit.reference.short", formatName(reference));
    }

    protected String formatName(Entity entity){
        if(entity instanceof User u){
            return u.getTag();
        }else if(entity instanceof GuildChannel g){
            return g.getName();
        }else{
            throw new UnsupportedOperationException("Unknown entity type.");
        }
    }

    protected String getReferenceContent(ContextView context, Entity entity){
        return messageService.format(context, "audit.reference", formatName(entity),
                (entity instanceof GuildChannel ? "<#" : "<@") + entity.getId() + ">");
    }

    protected abstract void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed);
}
