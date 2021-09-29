package inside.interaction.user.common;

import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.interaction.InteractionUserEnvironment;
import inside.interaction.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@InteractionUserCommand(name = "Get avatar")
public class AvatarCommand extends BaseUserCommand{
    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(InteractionUserEnvironment env){
        return env.getTargetUser(EntityRetrievalStrategy.REST)
                .flatMap(user -> messageService.info(env, "command.avatar.text", user.getUsername(), user.getMention())
                        .withEmbeds(EmbedCreateSpec.builder()
                                .description(messageService.format(env.context(),
                                        "command.avatar.text", user.getUsername(), user.getMention()))
                                .image(user.getAvatarUrl() + "?size=512")
                                .color(settings.getDefaults().getNormalColor())
                                .build()));
    }
}
