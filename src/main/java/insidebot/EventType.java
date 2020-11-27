package insidebot;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import insidebot.data.entity.LocalMember;
import reactor.util.annotation.*;

import java.util.*;

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

        @Override
        public String toString(){
            return "MessageClearEvent{" +
                   "history=" + history +
                   ", channel=" + channel +
                   ", user=" + user +
                   ", count=" + count +
                   '}';
        }
    }

    public static class MemberUnmuteEvent{
        public final @NonNull LocalMember userInfo;

        public MemberUnmuteEvent(@NonNull LocalMember userInfo){
            this.userInfo = userInfo;
        }

        @Override
        public String toString(){
            return "MemberUnmuteEvent{" +
                   "userInfo=" + userInfo +
                   '}';
        }
    }

    public static class MemberMuteEvent{
        public final @NonNull User user;
        public final @Nullable Member member;
        public final int delay;

        public MemberMuteEvent(@NonNull LocalMember userInfo, int delay){
            this.user = /*userInfo.asUser().block()*/null; //todo
            this.member = /*userInfo.asMember().block()*/null;
            this.delay = delay;
        }

        @Override
        public String toString(){
            return "MemberMuteEvent{" +
                   "user=" + user +
                   ", member=" + member +
                   ", delay=" + delay +
                   '}';
        }
    }
}