package inside.voice;

import com.github.benmanes.caffeine.cache.*;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import inside.util.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class VoiceServiceImpl implements VoiceService{
    private final Cache<Snowflake, VoiceRegistry> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofDays(3))
            .build();

    private final Lazy<AudioPlayerManager> playerManager = Lazy.of(() -> {
        DefaultAudioPlayerManager pm = new DefaultAudioPlayerManager();
        pm.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(pm);
        return pm;
    });

    @Override
    public VoiceRegistry getOrCreate(Snowflake guildId){
        return cache.get(guildId, id -> {
            AudioPlayer player = getAudioPlayerManager().createPlayer();
            return VoiceRegistry.create(id, player, LavaplayerAudioProvider.create(player));
        });
    }

    @Override
    public AudioPlayerManager getAudioPlayerManager(){
        return playerManager.get();
    }
}
