package inside.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import inside.Configuration;
import inside.interaction.chatinput.common.TicTacToeGame;

import java.util.Optional;

public class GameService extends BaseService {

    // userId -> game
    private final Cache<Snowflake, TicTacToeGame> games;

    public GameService(GatewayDiscordClient client, Configuration configuration) {
        super(client);

        games = Caffeine.newBuilder()
                .expireAfterWrite(configuration.discord().awaitComponentTimeout())
                .build();
    }

    public Optional<TicTacToeGame> getTTTGame(Snowflake userId) {
        return Optional.ofNullable(games.getIfPresent(userId));
    }

    public void registerGame(TicTacToeGame game) {
        games.put(game.getXUserId(), game);
        games.put(game.getOUserId(), game);
    }

    public void removeGame(TicTacToeGame game) {
        games.invalidate(game.getXUserId());
        games.invalidate(game.getOUserId());
    }
}
