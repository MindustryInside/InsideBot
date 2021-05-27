package inside.voice;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.*;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackLoader extends AudioEventAdapter{
    private final ConcurrentLinkedQueue<AudioTrack> queue = new ConcurrentLinkedQueue<>();
    private final AudioPlayer player;

    protected TrackLoader(AudioPlayer player){
        this.player = Objects.requireNonNull(player, "player");
    }

    public void queue(AudioTrack track){
        if(!player.startTrack(track, true)){
            queue.offer(track);
        }
    }

    public void queue(AudioPlaylist playlist){
        for(AudioTrack track : playlist.getTracks()){
            queue(track);
        }
    }

    public void clear(){
        queue.clear();
        player.stopTrack();
    }

    public void nextTrack(){
        player.startTrack(queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason){
        if(endReason.mayStartNext){
            nextTrack();
        }
    }
}
