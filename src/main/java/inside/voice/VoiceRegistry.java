package inside.voice;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.common.util.Snowflake;
import discord4j.voice.AudioProvider;

import java.util.Objects;

public class VoiceRegistry{
    private final Snowflake guildId;
    private final AudioPlayer player;
    private final AudioProvider audioProvider;
    private final TrackLoader trackLoader;

    private VoiceRegistry(Snowflake guildId, AudioPlayer player, AudioProvider audioProvider){
        this.guildId = Objects.requireNonNull(guildId, "guildId");
        this.player = Objects.requireNonNull(player, "player");
        this.audioProvider = Objects.requireNonNull(audioProvider, "audioProvider");

        trackLoader = new TrackLoader(player);
    }

    public static VoiceRegistry create(Snowflake guildId, AudioPlayer player, AudioProvider audioProvider){
        return new VoiceRegistry(guildId, player, audioProvider);
    }

    public Snowflake getGuildId(){
        return guildId;
    }

    public AudioPlayer getPlayer(){
        return player;
    }

    public AudioProvider getAudioProvider(){
        return audioProvider;
    }

    public TrackLoader getTrackLoader(){
        return trackLoader;
    }
}
