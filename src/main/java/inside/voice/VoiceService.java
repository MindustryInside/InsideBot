package inside.voice;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.common.util.Snowflake;

public interface VoiceService{

    VoiceRegistry getOrCreate(Snowflake guildId);

    AudioPlayerManager getAudioPlayerManager();
}
