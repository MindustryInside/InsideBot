package insidebot;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import insidebot.data.model.UserInfo;
import reactor.util.annotation.*;

import java.util.List;

public class EventType{
    public static class MessageClearEvent{
        public final @NonNull List<Message> history;
        public final @NonNull TextChannel channel;
        public final @NonNull User user;
        public final int count;

        public MessageClearEvent(@NonNull List<Message> history, @NonNull User user, @NonNull TextChannel channel, int count){
            this.history = history;
            this.channel = channel;
            this.user = user;
            this.count = count;
        }
    }

    public static class MemberUnmuteEvent{
        public final @NonNull UserInfo userInfo;

        public MemberUnmuteEvent(@NonNull UserInfo userInfo){
            this.userInfo = userInfo;
        }
    }

    public static class MemberMuteEvent{
        public final @NonNull User user;
        public final @Nullable Member member;
        public final int delay;

        public MemberMuteEvent(@NonNull UserInfo userInfo, int delay){
            this.user = userInfo.asUser();
            this.member = userInfo.asMember();
            this.delay = delay;
        }
    }

    public static class MemberBanEvent{
        public final @NonNull UserInfo userInfo;

        public MemberBanEvent(@NonNull UserInfo userInfo){
            this.userInfo = userInfo;
        }
    }
}
