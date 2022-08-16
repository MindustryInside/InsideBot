package inside.interaction.chatinput.moderation;

import discord4j.core.object.entity.Member;
import discord4j.discordjson.Id;
import inside.data.EntityRetriever;
import inside.data.entity.base.ConfigEntity;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.service.MessageService;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static reactor.bool.BooleanUtils.or;

public abstract class ModerationCommand extends InteractionGuildCommand {

    protected final EntityRetriever entityRetriever;

    public ModerationCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = entityRetriever;
    }

    @Override
    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {

        Member author = env.event().getInteraction().getMember().orElseThrow();

        return entityRetriever.getModerationConfigById(author.getGuildId())
                .filter(ConfigEntity::enabled)
                .switchIfEmpty(messageService.err(env, "common.moderation-disabled").then(Mono.never()))
                .flatMap(config -> or(super.filter(env), Mono.justOrEmpty(author.getRoleIds())
                        .flatMapIterable(Function.identity())
                        .any(id -> config.adminRoleIds().map(s -> s.contains(Id.of(id.asLong()))).orElse(false))));
    }
}
