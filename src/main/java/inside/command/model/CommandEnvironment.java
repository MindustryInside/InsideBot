package inside.command.model;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import inside.data.entity.LocalMember;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Objects;

public class CommandEnvironment{
    private final Member member;
    private final Message message;
    private final ContextView context; // TODO(Skat): use Mono#deferContextual
    private final LocalMember localMember;

    private CommandEnvironment(Member member, Message message, ContextView context, LocalMember localMember){
        this.member = Objects.requireNonNull(member, "member");
        this.message = Objects.requireNonNull(message, "message");
        this.context = Objects.requireNonNull(context, "context");
        this.localMember = Objects.requireNonNull(localMember, "localMember");
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

        public CommandEnvironment build(){
            return new CommandEnvironment(member, message, context, localMember);
        }
    }
}
