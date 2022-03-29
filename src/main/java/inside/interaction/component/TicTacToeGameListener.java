package inside.interaction.component;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ButtonInteractionEnvironment;
import inside.interaction.annotation.ComponentProvider;
import inside.interaction.chatinput.common.TicTacToeGame;
import inside.service.GameService;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ComponentProvider("ox-game")
public class TicTacToeGameListener implements ButtonListener {

    private final MessageService messageService;
    private final GameService gameService;

    public TicTacToeGameListener(MessageService messageService, GameService gameService) {
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.gameService = Objects.requireNonNull(gameService, "gameService");
    }

    @Override
    public Publisher<?> handle(ButtonInteractionEnvironment env) {
        String str = env.event().getCustomId();
        Snowflake userId = env.event().getInteraction().getUser().getId();

        Message message = env.event().getMessage().orElseThrow();

        return Mono.justOrEmpty(gameService.getTTTGame(userId))
                .flatMap(g -> {
                    boolean selfGame = g.getXUserId().equals(g.getOUserId());
                    boolean xs = selfGame ? !g.isLastX() : userId.equals(g.getXUserId());
                    if (!selfGame && xs == g.isLastX()) {
                        return messageService.err(env, "Вы уже сделали свой ход, дождитесь пока соперник сделает свой").then(Mono.never());
                    }

                    if (str.equals("inside-ox-game-exit")) {
                        boolean isX = userId.equals(g.getXUserId());
                        String res = "\n**Итог:** победил %s".formatted(selfGame ? g.isLastX() ? "**o**" : "**x**"
                                : MessageUtil.getUserMention(isX ? g.getOUserId() : g.getXUserId()) +
                                " (**" + (isX ? "o" : "x") +  "**)");

                        gameService.removeGame(g);
                        String xUserStr = MessageUtil.getUserMention(g.getXUserId()) + (!selfGame ? " (**x**)" : "");
                        String oUserStr = selfGame ? "самим с собой" : "с " + MessageUtil.getUserMention(g.getOUserId()) + " (**o**)";

                        return env.event().edit()
                                .withEmbeds(List.of(EmbedCreateSpec.builder()
                                        .title("Крестики-Нолики")
                                        .description("Игра %s %s%s\n\n%s".formatted(xUserStr, oUserStr, res, g.asText()))
                                        .color(env.configuration().discord().embedColor())
                                        .build()))
                                .withComponents(List.of());
                    }

                    int idx = Integer.parseInt(str.substring("inside-ox-game-".length()));
                    int x = idx / 3;
                    int y = idx % 3;

                    if (!g.play(x, y, xs)) {
                        // Никогда не произойдёт
                        return messageService.err(env, "На этой ячейке уже есть знак").then(Mono.never());
                    }

                    TicTacToeGame.State st = g.state();
                    String res = switch (st) {
                        case NEUTRAL -> "\n**Итог:** Ничья";
                        case O_WIN, X_WIN -> "\n**Итог:** победил %s".formatted(selfGame ? g.isLastX() ? "**x**" : "**o**"
                                : MessageUtil.getUserMention(st == TicTacToeGame.State.O_WIN ? g.getOUserId() : g.getXUserId()) +
                                " (**" + (st == TicTacToeGame.State.O_WIN ? "o" : "x") +  "**)");
                        default -> "";
                    };

                    if (st != TicTacToeGame.State.PLAYING) {
                        gameService.removeGame(g);
                    }

                    String xUserStr = MessageUtil.getUserMention(g.getXUserId()) + (!selfGame ? " (**x**)" : "");
                    String oUserStr = selfGame ? "самим с собой" : "с " + MessageUtil.getUserMention(g.getOUserId()) + " (**o**)";
                    String next = st == TicTacToeGame.State.PLAYING ? "\n**Текущий ход:** " + (g.isLastX() ? "o" : "x") : "";

                    return env.event().edit()
                            .withEmbeds(List.of(EmbedCreateSpec.builder()
                                    .title("Крестики-Нолики")
                                    .description("Игра %s %s%s%s\n\n%s".formatted(xUserStr, oUserStr,
                                            next, res, g.asText()))
                                    .color(env.configuration().discord().embedColor())
                                    .build()))
                            .withComponents(st != TicTacToeGame.State.PLAYING ? List.of() : message.getComponents().stream()
                                    .map(l -> l.getChildren().stream()
                                            .map(c -> (Button) c)
                                            .map(c -> {
                                                String s = c.getCustomId().orElseThrow();
                                                if (s.equals("inside-ox-game-" + idx)) {
                                                    return c.disabled();
                                                }
                                                return c;
                                            })
                                            .collect(Collectors.toList()))
                                    .map(ActionRow::of)
                                    .collect(Collectors.toList()));
                });
    }
}
