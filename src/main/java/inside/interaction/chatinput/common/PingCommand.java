package inside.interaction.chatinput.common;

import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;

@ChatInputCommand(name = "ping", description = "Получить задержку ответа бота.")
public class PingCommand extends InteractionCommand {

    public PingCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        long start = System.currentTimeMillis();

        return env.event().deferReply().then(env.event().createFollowup("Подождите..."))
                .flatMap(message -> env.event().editFollowup(message.getId())
                        .withContentOrNull(String.format("Понг! %sмс", System.currentTimeMillis() - start)));
    }
}
