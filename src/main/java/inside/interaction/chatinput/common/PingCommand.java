package inside.interaction.chatinput.common;

import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;

@ChatInputCommand(value = "commands.common.ping")
public class PingCommand extends InteractionCommand {

    public PingCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        long start = System.currentTimeMillis();

        return env.event().deferReply().then(env.event().createFollowup(
                messageService.get(env.context(), "commands.common.ping.testing")))
                .flatMap(message -> env.event().editFollowup(message.getId())
                        .withContentOrNull(messageService.format(env.context(), "commands.common.ping.completed",
                                System.currentTimeMillis() - start)));
    }
}
