package inside.interaction.chatinput.moderation;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.data.EntityRetriever;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Option;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.time.Instant;

@ChatInputCommand(value = "commands.moderation.delete", permissions = PermissionCategory.MODERATOR)
@Option(name = "count", type = Type.INTEGER, minValue = 1, maxValue = DeleteCommand.MAX_DELETED_MESSAGES, required = true)
public class DeleteCommand extends ModerationCommand {

    protected static final int MAX_DELETED_MESSAGES = 100;

    public DeleteCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        int count = env.getOption("count")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Math::toIntExact)
                .orElseThrow();

        Instant timeLimit = Instant.now().minus(Duration.ofDays(14L));

        return env.event().getInteraction().getChannel()
                .ofType(GuildMessageChannel.class)
                .zipWhen(c -> Mono.justOrEmpty(c.getLastMessageId()))
                .flatMapMany(TupleUtils.function((channel, lastMessageId) -> channel.getMessagesBefore(lastMessageId)
                        .take(count, true)
                        .filter(m -> m.getTimestamp().isAfter(timeLimit))
                        .switchIfEmpty(messageService.err(env, "commands.moderation.delete.could-not-delete").thenMany(Flux.never()))
                        .collectList()
                        .flatMap(m -> Mono.defer(() -> {
                            if (m.size() == 1) {
                                return m.get(0).delete();
                            }
                            return channel.bulkDeleteMessages(Flux.fromIterable(m)).then();
                        }).then(messageService.text(env, "commands.moderation.delete.successful", m.size(),
                                messageService.getPluralized(env.context(), "common.message", m.size()))))));
    }
}
