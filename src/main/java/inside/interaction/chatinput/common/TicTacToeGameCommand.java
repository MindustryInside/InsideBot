package inside.interaction.chatinput.common;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.GameService;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static inside.service.InteractionService.CUSTOM_ID_PREFIX;
import static inside.service.InteractionService.applyCustomId;

@ChatInputCommand(name = "ox", description = "Начать игру в крестики-нолики.")
public class TicTacToeGameCommand extends InteractionCommand {

    public static final int SIZE = 3;

    private static List<LayoutComponent> rows;

    static {
        List<LayoutComponent> rows = new ArrayList<>();
        List<ActionComponent> components = new ArrayList<>();
        for (int i = 1, c = SIZE * SIZE; i <= c; i++) {
            components.add(Button.primary(CUSTOM_ID_PREFIX + "ox-game-" + (i - 1),
                    ReactionEmoji.unicode(i + "\u20E3")));

            if (i % 3 == 0) {
                rows.add(ActionRow.of(components));
                components.clear();
            }
        }

        rows.add(ActionRow.of(Button.danger(CUSTOM_ID_PREFIX + "ox-game-exit", "Закончить игру")));

        TicTacToeGameCommand.rows = Collections.unmodifiableList(rows);
    }

    private final GameService gameService;

    public TicTacToeGameCommand(MessageService messageService, GameService gameService) {
        super(messageService);
        this.gameService = Objects.requireNonNull(gameService, "gameService");

        addOption(builder -> builder.name("sign")
                .description("Каким знаком играть. По умолчанию крестиком")
                .choices(ApplicationCommandOptionChoiceData.builder()
                                .name("x")
                                .value("x")
                                .build(),
                        ApplicationCommandOptionChoiceData.builder()
                                .name("o")
                                .value("o")
                                .build())
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        addOption(builder -> builder.name("self-game")
                .description("Начать игру самим с собой")
                .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        Snowflake userId = env.event().getInteraction().getUser().getId();

        if (gameService.getTTTGame(userId).isPresent()) {
            return messageService.err(env, "Сначала закончите активную игру");
        }

        boolean xSign = env.getOption("sign")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map("x"::equals)
                .orElse(true);

        boolean selfGame = env.getOption("self-game")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(false);

        String userCustomId = applyCustomId("ox-user");
        String exitCustomId = applyCustomId("ox-exit");

        if (selfGame) {

            var g = new TicTacToeGame(SIZE, userId, userId, xSign);

            gameService.registerGame(g);

            return env.event().deferReply().then(env.event().editReply()
                    .withEmbedsOrNull(List.of(EmbedCreateSpec.builder()
                            .title("Крестики-Нолики")
                            .description("Игра %s самим с собой\n**Текущий ход:** %s\n\n%s".formatted(
                                    MessageUtil.getUserMention(userId), xSign ? "x" : "o", g.asText()))
                            .color(env.configuration().discord().embedColor())
                            .build()))
                    .withComponentsOrNull(rows));
        }

        Mono<Void> onExit = env.interactionService().awaitButtonInteraction(userId, exitCustomId,
                cenv -> cenv.event().deferEdit().then(cenv.event().deleteReply()));
        Mono<Void> onSearch = env.interactionService().awaitButtonInteraction(userCustomId,
                cenv -> {
                    Snowflake id = cenv.event().getInteraction().getUser().getId();
                    if (id.equals(userId)) {
                        return messageService.err(cenv, "Вы не можете так начать игру самим с собой");
                    }

                    Snowflake xUserId = xSign ? userId : id;
                    Snowflake oUserId = xSign ? id : userId;

                    var g = new TicTacToeGame(SIZE, xUserId, oUserId, xSign);

                    gameService.registerGame(g);

                    String xUserStr = MessageUtil.getUserMention(xUserId) + " (**x**)";
                    String oUserStr = "с " + MessageUtil.getUserMention(oUserId) + " (**o**)";

                    return cenv.event().edit()
                            .withEmbeds(List.of(EmbedCreateSpec.builder()
                                    .title("Крестики-Нолики")
                                    .description("Игра %s %s\n**Текущий ход:** %s\n\n%s".formatted(xUserStr, oUserStr,
                                            xSign ? "x" : "o", g.asText()))
                                    .color(env.configuration().discord().embedColor())
                                    .build()))
                            .withComponents(rows);
                });

        return env.event().reply("Выбор оппонента для игры")
                .withComponents(List.of(ActionRow.of(
                        Button.primary(userCustomId, "Стать оппонентом"),
                        Button.danger(exitCustomId, "Завершить поиск"))))
                .and(Mono.firstWithSignal(onExit, onSearch));
    }
}
