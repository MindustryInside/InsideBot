package inside.voice;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.immutables.builder.Builder;

public class YoutubeVideoInfo extends AudioTrackInfo{

    public YoutubeVideoInfo(){
        super(null, null, 0, null, false, null);
    }

    @Builder.Constructor
    public YoutubeVideoInfo(String title, String author, long length, String identifier, boolean isStream, String uri){
        super(title, author, length, identifier, isStream, uri);
    }

    public static YoutubeVideoInfoBuilder builder(){
        return new YoutubeVideoInfoBuilder();
    }
}
