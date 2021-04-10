package inside.command.model;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import inside.data.entity.LocalMember;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.*;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.Supplier;

public class CommandEnvironment{
    private final Member member;
    private final Message message;
    private final ContextView context; // TODO(Skat): use Mono#deferContextual
    private final LocalMember localMember;

    CommandEnvironment(Builder builder){
        Objects.requireNonNull(builder, "builder");
        this.member = builder.member;
        this.message = builder.message;
        this.context = builder.context;
        this.localMember = builder.localMember;
    }

    public static Builder builder(){
        return new Builder();
    }

    public Member getAuthorAsMember(){
        return member;
    }

    public Message getMessage(){
        return message;
    }

    public ContextView context(){
        return context;
    }

    public LocalMember getLocalMember(){
        return localMember;
    }

    public Mono<MessageChannel> getReplyChannel(){
        return getMessage().getChannel();
    }

    public GatewayDiscordClient getClient(){
        return getMessage().getClient();
    }

    public static class Builder{
        private Member member;
        private Message message;
        private ContextView context;
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
            this.context = Objects.requireNonNull(context, "context");
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

        public CommandEnvironment build(){
            if(channel == null){
                channel = () -> message.getChannel();
            }
            if(scheduler == null){
                scheduler = Schedulers.boundedElastic();
            }
            return new CommandEnvironment(this);
        }
    }
}
