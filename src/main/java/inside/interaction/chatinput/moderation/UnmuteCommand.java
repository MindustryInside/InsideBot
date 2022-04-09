package inside.interaction.chatinput.moderation;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Optional;

@ChatInputCommand(name = "unmute", description = "Снятие мута с пользователя.", permissions = PermissionCategory.MODERATOR)
public class UnmuteCommand extends ModerationCommand {

    public UnmuteCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addOption(builder -> builder.name("target")
                .description("Нарушитель правил.")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(true));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {

        Member author = env.event().getInteraction().getMember().orElseThrow();
        Snowflake guildId = author.getGuildId();

        Snowflake targetId = env.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                .orElseThrow();

        return entityRetriever.getModerationConfigById(guildId)
                .zipWith(env.event().getClient().getMemberById(guildId, targetId)
                        .filterWhen(member -> entityRetriever.moderationActionCountById(
                                ModerationAction.Type.mute, guildId, member.getId())
                                .map(l -> l > 0))
                        .switchIfEmpty(messageService.err(env, "Пользователь не находится в мьюте").then(Mono.never()))
                        .filterWhen(u -> u.getBasePermissions()
                                .map(p -> p.equals(PermissionSet.all()) || !p.contains(Permission.ADMINISTRATOR)))
                        .switchIfEmpty(messageService.err(env, "Вы не можете размьютить администраторов").then(Mono.never())))
                .flatMap(TupleUtils.function((config, target) -> {

                    Mono<Void> delete = entityRetriever.getAllModerationActionById(ModerationAction.Type.mute, guildId, targetId)
                            .single()
                            .flatMap(entityRetriever::delete);

                    Mono<Void> unmute = Mono.justOrEmpty(target.getCommunicationDisabledUntil())
                            .switchIfEmpty(Mono.justOrEmpty(config.muteRoleId())
                                    .map(Snowflake::of)
                                    .flatMap(target::removeRole)
                                    .then(Mono.empty()))
                            .flatMap(i -> target.edit().withCommunicationDisabledUntil(
                                    Possible.of(Optional.empty())))
                            .then();

                    return messageService.text(env, "Пользователь **%s** размьючен", MessageUtil.getUserMention(targetId))
                            .and(unmute)
                            .and(delete);
                }));
    }
}
