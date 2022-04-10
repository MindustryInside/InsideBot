package inside.service.task;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.Launcher;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

// 🛰️ 📡 🛰️ 📡 🛰️ 📡 🛰️ 📡 🛰️ 📡 🛰️ 📡 🛰️ 📡 🛰️ 📡
public class AliveForeverThreadTask implements Task {
    private static final String[] emojis = {"🛰️", "📡"};

    private final Snowflake channelId;

    private final AtomicBoolean receive = new AtomicBoolean(
            ThreadLocalRandom.current().nextBoolean());

    public AliveForeverThreadTask(Snowflake channelId) {
        this.channelId = channelId;
    }

    @Override
    public Publisher<?> execute() {
        return Launcher.getClient().getChannelById(channelId)
                .ofType(GuildMessageChannel.class)
                .flatMap(c -> c.createMessage(emojis[receive.getAndSet(!receive.get()) ? 1 : 0]));
    }

    @Override
    public Duration getInterval() {
        return Duration.ofHours(6);
    }
}
