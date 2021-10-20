package inside.interaction.user.common;

import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.interaction.UserEnvironment;
import inside.interaction.annotation.UserCommand;
import inside.interaction.user.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@UserCommand(name = "Get avatar")
public class AvatarCommand extends BaseUserInteractionCommand{
    @Autowired
    private Settings settings;

    @Override
    public Publisher<?> execute(UserEnvironment env){
        return Mono.justOrEmpty(env.event().getResolvedUser())
                .flatMap(user -> messageService.info(env, "command.avatar.text", user.getUsername(), user.getMention())
                        .withEmbeds(EmbedCreateSpec.builder()
                                .description(messageService.format(env.context(),
                                        "command.avatar.text", user.getUsername(), user.getMention()))
                                .image(user.getAvatarUrl() + "?size=512")
                                .color(settings.getDefaults().getNormalColor())
                                .build()));
    }
}
