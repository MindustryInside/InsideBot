package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.Configuration;
import inside.command.CommandEnvironment;
import inside.command.CommandHandler;
import inside.data.EntityRetriever;
import inside.data.entity.base.ConfigEntity;
import inside.service.InteractionService;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.Instant;

import static inside.util.ContextUtil.KEY_LOCALE;
import static inside.util.ContextUtil.KEY_TIMEZONE;
import static reactor.function.TupleUtils.function;

public class MessageEventHandler extends ReactiveEventAdapter {

    private final EntityRetriever entityRetriever;
    private final CommandHandler commandHandler;
    private final Configuration configuration;
    private final InteractionService interactionService;
    private final MessageService messageService;

    public MessageEventHandler(EntityRetriever entityRetriever, CommandHandler commandHandler,
                               Configuration configuration, InteractionService interactionService,
                               MessageService messageService) {
        this.entityRetriever = entityRetriever;
        this.commandHandler = commandHandler;
        this.configuration = configuration;
        this.interactionService = interactionService;
        this.messageService = messageService;
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        if (member == null || member.isBot() || message.getContent().isEmpty() ||
                message.isTts() || message.getType() != Message.Type.DEFAULT) {
            return Mono.empty();
        }

        Mono<Void> saveActivity = entityRetriever.getActivityConfigById(member.getGuildId())
                .filter(ConfigEntity::enabled)
                .zipWith(entityRetriever.getActivityById(member.getGuildId(), member.getId())
                        .switchIfEmpty(entityRetriever.createActivity(member.getGuildId(), member.getId()))
                        .map(ac -> ac.incrementMessageCount()
                                .withLastSentMessage(message.getTimestamp())))
                .flatMap(function((config, activity) -> Mono.defer(() -> {
                    Snowflake roleId = Snowflake.of(config.roleId());
                    if (activity.messageCount() >= config.messageThreshold() && activity.lastSentMessage()
                            .map(i -> i.isAfter(Instant.now().minus(config.countingInterval())))
                            .orElse(false)) {
                        if (member.getRoleIds().contains(roleId)) {
                            return Mono.just(activity);
                        }

                        return member.addRole(roleId)
                                .thenReturn(activity);
                    }

                    return Mono.just(activity);
                })
                .then(entityRetriever.save(activity))))
                .then();

        Mono<Context> initContext = entityRetriever.getGuildConfigById(member.getGuildId())
                .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timezone()));

        Mono<Void> command = message.getChannel()
                .cast(GuildMessageChannel.class)
                .zipWith(initContext)
                .flatMap(TupleUtils.function((channel, ctx) -> commandHandler.handle(CommandEnvironment.builder()
                        .member(member)
                        .message(message)
                        .channel(channel)
                        .context(ctx)
                        .configuration(configuration)
                        .interactionService(interactionService)
                        .messageService(messageService)
                        .build())));

        return Mono.when(command, saveActivity);
    }
}
