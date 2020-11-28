package insidebot.event.dispatcher;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.LocalMember;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.annotation.*;

import java.util.*;

/**
 * Все внутренние ивенты
 */
public final class EventType{

    public static class MessageClearEvent extends BaseEvent{
        public final @NonNull List<Message> history;
        public final @NonNull TextChannel channel;
        public final @NonNull User user;
        public final int count;

        public MessageClearEvent(Guild guild, @NonNull List<Message> history, @NonNull User user, @NonNull TextChannel channel, int count){
            super(guild);
            this.history = history;
            this.channel = channel;
            this.user = user;
            this.count = count;
        }

        @Override
        public String toString(){
            return "MessageClearEvent{" +
                   "history=" + history +
                   ", channel=" + channel +
                   ", user=" + user +
                   ", count=" + count +
                   ", guild=" + guild +
                   '}';
        }
    }

    public static class MemberUnmuteEvent extends BaseEvent{
        public final @NonNull LocalMember localMember;

        public MemberUnmuteEvent(Guild guild, @NonNull LocalMember userInfo){
            super(guild);
            this.localMember = userInfo;
        }

        @Override
        public String toString(){
            return "MemberUnmuteEvent{" +
                   "localMember=" + localMember +
                   ", guild=" + guild +
                   '}';
        }
    }

    public static class MemberMuteEvent extends BaseEvent{
        public final @NonNull LocalMember localMember;
        public final int delay;

        @Autowired
        private DiscordService discordService;

        public MemberMuteEvent(Guild guild, @NonNull LocalMember localMember, int delay){
            super(guild);
            this.localMember = localMember;
            this.delay = delay;
        }

        @Override
        public String toString(){
            return "MemberMuteEvent{" +
                   "localMember=" + localMember +
                   ", delay=" + delay +
                   ", guild=" + guild +
                   '}';
        }
    }
}
