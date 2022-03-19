package inside.interaction.chatinput.guild;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.possible.Possible;
import inside.data.EntityRetriever;
import inside.data.entity.Activity;
import inside.data.entity.base.ConfigEntity;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.interaction.util.MessagePaginator;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@ChatInputCommand(name = "leaderboard", description = "Отобразить список активных пользователей.")
public class LeaderboardCommand extends InteractionGuildCommand {

    public static final int PER_PAGE = 10;

    private final EntityRetriever entityRetriever;

    public LeaderboardCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        Snowflake authorId = env.event().getInteraction().getUser().getId();
        Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();
        AtomicBoolean seenAuthor = new AtomicBoolean();

        Function<Activity, String> pattern = wallet -> "**%d.** %s " +
                (wallet.userId() == authorId.asLong() ? " (**вы**)" : "") + " - %d %n";

        Function<MessagePaginator.Page, ? extends Mono<MessageCreateSpec>> paginator = page ->
                entityRetriever.getAllActivityInGuild(guildId)
                        .sort(Comparator.comparingLong(Activity::messageCount)
                                .thenComparing(a -> a.incrementMessageCount()
                                        .lastSentMessage().orElse(Instant.MIN))
                                .reversed())
                        .index().skip(page.getPage() * PER_PAGE).take(PER_PAGE, true)
                        .doOnNext(TupleUtils.consumer((idx, activity) -> {
                            if (activity.userId() == authorId.asLong()) {
                                seenAuthor.set(true);
                            }
                        }))
                        .map(TupleUtils.function((idx, activity) -> String.format(pattern.apply(activity),
                                idx + 1, MessageUtil.getUserMention(activity.userId()),
                                activity.messageCount())))
                        .collect(Collectors.joining())
                        .flatMap(str -> {
                            if (seenAuthor.get()) {
                                seenAuthor.set(false);
                                return Mono.just(str);
                            }

                            return entityRetriever.getPositionAndActivityById(guildId, authorId)
                                    .map(TupleUtils.function((idx, activity) -> String.format(pattern.apply(activity),
                                            idx - 1, MessageUtil.getUserMention(activity.userId()),
                                            activity.messageCount())))
                                    .map(footer -> str + "**...**\n" + footer);
                        })
                        .map(str -> MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .title("Таблица активных пользователей (сообщения)")
                                        .description(str)
                                        .color(env.configuration().discord().embedColor())
                                        .footer(String.format("Страница %s/%s", page.getPage() + 1, page.getPageCount()), null)
                                        .build())
                                .components(page.getItemsCount() > PER_PAGE
                                        ? Possible.of(List.of(ActionRow.of(
                                        page.previousButton(id -> Button.primary(id, "Предыдущая Страница")),
                                        page.nextButton(id -> Button.primary(id, "Следующая Страница")))))
                                        : Possible.absent())
                                .build());

        return entityRetriever.getActivityConfigById(guildId)
                .filter(ConfigEntity::enabled)
                .switchIfEmpty(messageService.err(env, "Награждение активности пользователей выключено").then(Mono.never()))
                .flatMap(c -> entityRetriever.activityCountInGuild(guildId))
                .filter(l -> l > 0)
                .switchIfEmpty(messageService.err(env, "Список активных пользователей пуст").then(Mono.never()))
                .flatMap(l -> MessagePaginator.paginate(env, l, PER_PAGE, paginator));
    }
}
