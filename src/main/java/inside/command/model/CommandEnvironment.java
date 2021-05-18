package inside.command.model;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import inside.data.entity.LocalMember;
import org.immutables.builder.Builder;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public class CommandEnvironment{
    private final Member member;
    private final Message message;
    private final ContextView context;
    private final LocalMember localMember;

    @Builder.Constructor
    protected CommandEnvironment(Member member, Message message, ContextView context, LocalMember localMember){
        this.member = member;
        this.message = message;
        this.context = context;
        this.localMember = localMember;
    }

    public static CommandEnvironmentBuilder builder(){
        return new CommandEnvironmentBuilder();
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
}
