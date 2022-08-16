package inside.interaction.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import inside.interaction.ButtonInteractionEnvironment;
import inside.interaction.InteractionEnvironment;
import inside.service.InteractionService;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static inside.util.Mathf.random;
import static inside.util.MessageUtil.toFollowupCreateSpec;
import static inside.util.MessageUtil.toReplyEditSpec;

// Немного переделанная версия этого пагинатора:
// https://github.com/Alex1304/botrino/blob/34a16c3841481f527a101a89e8b3547116b164c4/interaction/src/main/java/botrino/interaction/util/MessagePaginator.java#L49
public final class MessagePaginator {

    private static final Logger log = Loggers.getLogger(MessagePaginator.class);

    private static final String PREVIOUS_ID = "-previous";
    private static final String NEXT_ID = "-next";

    private MessagePaginator() {
    }

    public static Mono<Void> paginate(InteractionEnvironment env, int itemsCount, int perPage,
                                      Function<? super Page, ? extends Mono<MessageCreateSpec>> messageGenerator) {
        return paginate(env, 0, itemsCount, perPage, messageGenerator);
    }

    public static Mono<Void> paginate(InteractionEnvironment env, int initialPage, int itemsCount, int perPage,
                                      Function<? super Page, ? extends Mono<MessageCreateSpec>> messageGenerator) {
        return Mono.deferContextual(ctx -> {
            String baseCustomId = InteractionService.CUSTOM_ID_PREFIX + "paginator-" + Integer.toHexString(random.nextInt());
            AtomicInteger currentPage = new AtomicInteger();
            AtomicBoolean active = new AtomicBoolean(true);
            Snowflake userId = env.event().getInteraction().getUser().getId();

            ChoiceCallback previous = env0 -> {
                if (!active.get()) { // На всякий случай
                    return Mono.error(new IllegalStateException("Inactive paginator"));
                }
                int newPage = currentPage.decrementAndGet();
                return messageGenerator.apply(new Page(newPage, itemsCount, perPage, baseCustomId))
                        .zipWith(Mono.just(env0));
            };

            ChoiceCallback next = env0 -> {
                if (!active.get()) {
                    return Mono.error(new IllegalStateException("inactive paginator"));
                }
                int newPage = currentPage.incrementAndGet();
                return messageGenerator.apply(new Page(newPage, itemsCount, perPage, baseCustomId))
                        .zipWith(Mono.just(env0));
            };

            return messageGenerator.apply(new Page(initialPage, itemsCount, perPage, baseCustomId))
                    .flatMap(spec -> env.event().deferReply()
                            .then(env.event().createFollowup(toFollowupCreateSpec(spec)))) // Гениально придумано. Может стоит заменить тип на ReplyEditSpec?
                    .map(Message::getId)
                    .flatMap(messageId -> Mono.firstWithValue(
                                    env.interactionService().awaitButtonInteraction(userId, baseCustomId + PREVIOUS_ID, previous),
                                    env.interactionService().awaitButtonInteraction(userId, baseCustomId + NEXT_ID, next))
                            .flatMap(TupleUtils.function((spec, env0) -> env0.event().deferEdit().then(
                                    env.event().editFollowup(messageId, toReplyEditSpec(spec)))))
                            .repeat(active::get)
                            .timeout(env.configuration().discord().awaitComponentTimeout())
                            .onErrorResume(TimeoutException.class, e -> Mono.fromRunnable(
                                    () -> log.debug("Paginator {} timed out", baseCustomId)))
                            .doOnNext(v -> log.debug("Paginator {} terminated with success", baseCustomId))
                            .doOnError(e -> log.error("Paginator " + baseCustomId + " terminated with an error", e))
                            .doOnCancel(() -> log.debug("Paginator {} cancelled", baseCustomId))
                            .doFinally(signal -> messageGenerator.apply(
                                            new Page(currentPage.get(), itemsCount, perPage, baseCustomId))
                                    .flatMap(message -> env.event().editFollowup(messageId, toReplyEditSpec(message)))
                                    .subscribe(null, e -> log.error("Error in doFinally of paginator " + baseCustomId, e)))
                            .then());
        });
    }

    @FunctionalInterface
    private interface ChoiceCallback
            extends Function<ButtonInteractionEnvironment,
            Mono<Tuple2<MessageCreateSpec, ButtonInteractionEnvironment>>> {
    }

    public static final class Page {

        private final int page;
        private final int itemsCount;
        private final int perPage;
        private final String baseCustomId;

        private Page(int page, int itemsCount, int perPage, String baseCustomId) {
            this.page = page;
            this.itemsCount = itemsCount;
            this.perPage = perPage;
            this.baseCustomId = baseCustomId;
        }

        public int getPage() {
            return page;
        }

        public int getPageCount() {
            return (int) Math.ceil(itemsCount / (double) perPage);
        }

        public int getItemsCount() {
            return itemsCount;
        }

        public int getPerPage() {
            return perPage;
        }

        public Button previousButton(Function<? super String, ? extends Button> buttonFactory) {
            return buttonFactory.apply(baseCustomId + PREVIOUS_ID).disabled(page == 0);
        }

        public Button nextButton(Function<? super String, ? extends Button> buttonFactory) {
            int skipPages = page * perPage;
            boolean disabled = skipPages + perPage >= itemsCount || itemsCount < perPage;
            return buttonFactory.apply(baseCustomId + NEXT_ID).disabled(disabled);
        }
    }
}
