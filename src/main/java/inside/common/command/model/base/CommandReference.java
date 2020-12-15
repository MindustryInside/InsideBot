package inside.common.command.model.base;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.*;
import inside.data.entity.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.*;

import java.util.Objects;
import java.util.function.*;

public class CommandReference implements CommandRequest, CommandResponse{
    private final MessageCreateEvent event;
    private final Supplier<Mono<? extends MessageChannel>> replyChannel;
    private final Scheduler replyScheduler;
    private final LocalMember localMember;

    public CommandReference(MessageCreateEvent event, LocalMember localMember, Supplier<Mono<? extends MessageChannel>> replyChannel, Scheduler replyScheduler){
        this.event = event;
        this.localMember = localMember;
        this.replyChannel = replyChannel;
        this.replyScheduler = replyScheduler;
    }

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public MessageCreateEvent event(){
        return event;
    }

    @Override
    public LocalMember localMember(){
        return localMember;
    }

    @Override
    public Mono<MessageChannel> getReplyChannel(){
        return event.getMessage().getChannel();
    }

    @Override
    public Mono<PrivateChannel> getPrivateChannel(){
        return Mono.justOrEmpty(event.getMessage().getAuthor()).flatMap(User::getPrivateChannel);
    }

    @Override
    public CommandResponse withDirectMessage(){
        return new CommandReference(event, localMember, () -> getPrivateChannel().cast(MessageChannel.class), replyScheduler);
    }

    @Override
    public CommandResponse withReplyChannel(Mono<? extends MessageChannel> channelSource){
        return new CommandReference(event, localMember, () -> channelSource, replyScheduler);
    }

    @Override
    public CommandResponse withScheduler(Scheduler scheduler){
        return new CommandReference(event, localMember, replyChannel, scheduler);
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
        private MessageCreateEvent event;
        private LocalMember localMember;
        private Supplier<Mono<? extends MessageChannel>> channel;
        private Scheduler scheduler;

        public Builder event(MessageCreateEvent event){
            this.event = event;
            return this;
        }

        public Builder localMember(LocalMember localMember){
            this.localMember = localMember;
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
            Objects.requireNonNull(event, "event");
            Objects.requireNonNull(localMember, "localMember");
            if(channel == null){
                channel = () -> event.getMessage().getChannel();
            }
            if(scheduler == null){
                scheduler = Schedulers.boundedElastic();
            }
            return new CommandReference(event, localMember, channel, scheduler);
        }
    }
}
