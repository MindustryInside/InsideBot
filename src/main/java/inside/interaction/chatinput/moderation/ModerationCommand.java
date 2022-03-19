package inside.interaction.chatinput.moderation;

import discord4j.core.object.entity.Member;
import discord4j.discordjson.Id;
import inside.data.EntityRetriever;
import inside.data.entity.base.ConfigEntity;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.service.MessageService;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

import static reactor.bool.BooleanUtils.or;

public abstract class ModerationCommand extends InteractionGuildCommand {

    protected final EntityRetriever entityRetriever;

    public ModerationCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {

        Member author = env.event().getInteraction().getMember().orElseThrow();

        return entityRetriever.getModerationConfigById(author.getGuildId())
                .filter(ConfigEntity::enabled)
                .switchIfEmpty(messageService.err(env, "Функции модерирования выключены на этом сервере").then(Mono.never()))
                .flatMap(config -> or(super.filter(env), Mono.justOrEmpty(author.getRoleIds())
                        .flatMapIterable(Function.identity())
                        .map(id -> Id.of(id.asLong()))
                        .any(id -> config.adminRoleIds().map(s -> s.contains(id)).orElse(false))));
    }
}
