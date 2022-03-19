package inside.interaction.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.gateway.RequestGuildMembers;
import inside.command.CommandEnvironment;
import inside.service.InteractionService;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.Mathf.random;

public final class MemberSearch {

    private static final Logger log = Loggers.getLogger(MemberSearch.class);

    private static final int DEFAULT_LIMIT = 5;

    private MemberSearch() {
    }

    public static Mono<Member> search(CommandEnvironment env, String query) {
        return search(env, query, DEFAULT_LIMIT);
    }

    public static Mono<Member> search(CommandEnvironment env, String query, int limit) {
        return Mono.defer(() -> {

            String customId = InteractionService.CUSTOM_ID_PREFIX + "search-" + Integer.toHexString(random.nextInt());

            Snowflake guildId = env.channel().getGuildId();
            Snowflake authorId = env.member().getId();

            Function<List<Member>, Mono<Member>> startInteraction = list -> env.channel().createMessage(MessageCreateSpec.builder()
                            .content("Выберите пользователя.")
                            .addComponent(ActionRow.of(SelectMenu.of(customId, list.stream()
                                    .map(member -> SelectMenu.Option.of(member.getNickname()
                                            .map(s -> s + " (" + member.getUsername() + ")")
                                            .orElseGet(member::getUsername) + (env.member().getId().equals(member.getId())
                                            ? " (вы)" : ""), member.getId().asString()))
                                    .collect(Collectors.toList()))))
                            .build())
                    .flatMap(original -> env.interactionService()
                            .awaitSelectMenuInteraction(authorId, customId, e -> e.event()
                                    .deferEdit().then(e.event().deleteReply())
                                    .thenReturn(Snowflake.of(e.event().getValues().get(0)))
                                    .flatMap(id -> e.event().getClient().getMemberById(guildId, id)))
                            .timeout(env.configuration().discord().awaitComponentTimeout())
                            .onErrorResume(TimeoutException.class, e -> Mono.fromRunnable(
                                    () -> log.debug("Member search {} timed out", customId)))
                            .doOnNext(v -> log.debug("Member search {} terminated with success", customId))
                            .doOnError(e -> log.error("Member search " + customId + " terminated with an error", e))
                            .doOnCancel(() -> log.debug("Member search {} cancelled", customId)));

            return env.message().getClient().requestMembers(RequestGuildMembers.builder()
                            .limit(limit)
                            .query(query)
                            .guildId(guildId.asString())
                            .build())
                    .switchIfEmpty(env.messageService().err(env, "Пользователь с таким никнеймом не найден").then(Mono.never()))
                    .collectList()
                    .flatMap(list -> list.size() == 1 ? Mono.just(list.get(0)) : startInteraction.apply(list));
        });
    }
}
