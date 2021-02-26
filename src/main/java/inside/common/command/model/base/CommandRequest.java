package inside.common.command.model.base;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import inside.data.entity.LocalMember;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public interface CommandRequest{

    Member getAuthorAsMember();

    Message getMessage();

    ContextView context();

    LocalMember localMember();

    Mono<MessageChannel> getReplyChannel();

    Mono<PrivateChannel> getPrivateChannel();

    default GatewayDiscordClient getClient(){
        return getMessage().getClient();
    }
}
