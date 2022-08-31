package inside.interaction.chatinput.common;

import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;

@ChatInputCommand(name = "commands.ping.key", description = "commands.ping.desc2")
public class PingCommand extends InteractionCommand {

    public PingCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        long start = System.currentTimeMillis();

        return env.event().deferReply().then(env.event().createFollowup(messageService.get(null,"inside.static.wait")))
                .flatMap(message -> env.event().editFollowup(message.getId())
                        .withContentOrNull(String.format(messageService.get(null,"commands.ping.message"), System.currentTimeMillis() - start)));
    }
}
