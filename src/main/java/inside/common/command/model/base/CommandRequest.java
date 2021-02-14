package inside.common.command.model.base;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.rest.util.PermissionSet;
import inside.data.entity.LocalMember;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Optional;

public interface CommandRequest{

    MessageCreateEvent event();

    ContextView context();

    LocalMember localMember();

    Mono<MessageChannel> getReplyChannel();

    Mono<PrivateChannel> getPrivateChannel();

    default GatewayDiscordClient getClient(){
        return event().getClient();
    }

    default Message getMessage(){
        return event().getMessage();
    }

    default Optional<User> getAuthor(){
        return event().getMessage().getAuthor();
    }

    default Member getAuthorAsMember(){
        return event().getMember().orElse(null);
    }
}
