package inside.common.command.model.base;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import inside.data.entity.LocalMember;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.*;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.*;

public class CommandReference implements CommandRequest, CommandResponse{
    private final Member member;
    private final Message message;
    private final ContextView context; // TODO(Skat): use Mono#deferContextual
    private final Supplier<Mono<? extends MessageChannel>> replyChannel;
    private final Scheduler replyScheduler;
    private final LocalMember localMember;

    CommandReference(Member member, Message message, ContextView context,
                     Supplier<Mono<? extends MessageChannel>> replyChannel,
                     Scheduler replyScheduler, LocalMember localMember){
        this.member = member;
        this.message = message;
        this.context = context;
        this.replyChannel = replyChannel;
        this.replyScheduler = replyScheduler;
        this.localMember = localMember;
    }

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public Member getAuthorAsMember(){
        return member;
    }

    @Override
    public Message getMessage(){
        return message;
    }

    @Override
    public ContextView context(){
        return context;
    }

    @Override
    public LocalMember localMember(){
        return localMember;
    }

    @Override
    public Mono<MessageChannel> getReplyChannel(){
        return getMessage().getChannel();
    }

    @Override
    public Mono<PrivateChannel> getPrivateChannel(){
        return Mono.justOrEmpty(getMessage().getAuthor()).flatMap(User::getPrivateChannel);
    }

    @Override
    public CommandResponse withDirectMessage(){
        return new CommandReference(member, message, context, () -> getPrivateChannel().cast(MessageChannel.class), replyScheduler, localMember);
    }

    @Override
    public CommandResponse withReplyChannel(Mono<? extends MessageChannel> channelSource){
        return new CommandReference(member, message, context, () -> channelSource, replyScheduler, localMember);
    }

    @Override
    public CommandResponse withScheduler(Scheduler scheduler){
        return new CommandReference(member, message, context, replyChannel, scheduler, localMember);
    }

    @Override
    public Mono<Void> sendMessage(Consumer<? super MessageCreateSpec> spec){
        return replyChannel.get()
                .publishOn(replyScheduler)
                .flatMap(channel -> channel.createMessage(spec))
                .then();
    }

    @Override
    public Mono<Void> sendEmbed(Consumer<? super EmbedCreateSpec> spec){
        return replyChannel.get()
                .publishOn(replyScheduler)
                .flatMap(channel -> channel.createEmbed(spec))
                .then();
    }

    public static class Builder{
        private Member member;
        private Message message;
        private ContextView contextView;
        private LocalMember localMember;
        private Supplier<Mono<? extends MessageChannel>> channel;
        private Scheduler scheduler;

        public Builder member(Member member){
            this.member = Objects.requireNonNull(member, "member");
            return this;
        }

        public Builder message(Message message){
            this.message = Objects.requireNonNull(message, "message");
            return this;
        }

        public Builder context(ContextView context){
            this.contextView = Objects.requireNonNull(context, "context");
            return this;
        }

        public Builder localMember(LocalMember localMember){
            this.localMember = Objects.requireNonNull(localMember, "localMember");
            return this;
        }

        public Builder channel(Supplier<Mono<? extends MessageChannel>> channel){
            this.channel = channel;
            return this;
        }

        public Builder scheduler(Scheduler scheduler){
            this.scheduler = scheduler;
            return this;
        }

        public CommandReference build(){
            if(channel == null){
                channel = () -> message.getChannel();
            }
            if(scheduler == null){
                scheduler = Schedulers.boundedElastic();
            }
            return new CommandReference(member, message, contextView, channel, scheduler, localMember);
        }
    }
}
