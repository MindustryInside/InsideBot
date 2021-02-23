package inside.event.dispatcher;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import inside.data.entity.LocalMember;
import org.joda.time.DateTime;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.util.*;

public final class EventType{

    private EventType(){}

    public static class MessageClearEvent extends BaseEvent{
        private final List<Message> history;
        private final TextChannel channel;
        private final Member member;
        private final int count;

        public MessageClearEvent(Guild guild, List<Message> history, Member member, TextChannel channel, int count){
            super(guild);
            this.history = Objects.requireNonNull(history, "history");
            this.channel = Objects.requireNonNull(channel, "channel");
            this.member = Objects.requireNonNull(member, "member");
            this.count = count;
        }

        public List<Message> getHistory(){
            return history;
        }

        public TextChannel getChannel(){
            return channel;
        }

        public Member getMember(){
            return member;
        }

        public int getCount(){
            return count;
        }

        @Override
        public String toString(){
            return "MessageClearEvent{" +
                   "history=" + history +
                   ", channel=" + channel +
                   ", user=" + member +
                   ", count=" + count +
                   ", guild=" + guild +
                   '}';
        }
    }

    public static class MemberUnmuteEvent extends BaseEvent{
        private final LocalMember localMember;

        public MemberUnmuteEvent(Guild guild, LocalMember localMember){
            super(guild);
            this.localMember = Objects.requireNonNull(localMember, "localMember");
        }

        public LocalMember getLocalMember(){
            return localMember;
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
        private final LocalMember admin;
        private final LocalMember target;
        private final DateTime delay;
        private final @Nullable String reason;

        public MemberMuteEvent(Guild guild, LocalMember admin, LocalMember target, DateTime delay, @Nullable String reason){
            super(guild);
            this.admin = Objects.requireNonNull(admin, "admin");
            this.target = Objects.requireNonNull(target, "target");
            this.delay = Objects.requireNonNull(delay, "delay");
            this.reason = reason;
        }

        public LocalMember getAdmin(){
            return admin;
        }

        public LocalMember getTarget(){
            return target;
        }

        public DateTime getDelay(){
            return delay;
        }

        public Optional<String> reason(){
            return Optional.ofNullable(reason);
        }

        @Override
        public String toString(){
            return "MemberMuteEvent{" +
                   "admin=" + admin +
                   ", target=" + target +
                   ", reason='" + reason + '\'' +
                   ", delay=" + delay +
                   '}';
        }
    }
}
