package inside.interaction.chatinput.moderation;

import discord4j.common.util.Snowflake;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.command.*;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.possible.Possible;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.data.entity.base.BaseEntity;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.interaction.util.MessagePaginator;
import inside.service.MessageService;
import inside.util.InternalId;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@ChatInputCommand(name = "warns", description = "Отобразить список предупреждений.")
public class WarnsCommand extends InteractionGuildCommand {

    public static final int PER_PAGE = 10;

    private final EntityRetriever entityRetriever;

    public WarnsCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");

        addOption(builder -> builder.name("target")
                .description("Пользователь, чьи предупреждения нужно показать. По умолчанию ваши")
                .type(ApplicationCommandOption.Type.USER.getValue()));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {

        Member author = env.event().getInteraction().getMember().orElseThrow();
        Snowflake guildId = author.getGuildId();

        Snowflake targetId = env.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                .orElse(author.getId());

        String nickname = env.event().getInteraction().getCommandInteraction()
                .flatMap(ApplicationCommandInteraction::getResolved)
                .flatMap(c -> c.getMember(targetId))
                .map(ResolvedMember::getDisplayName)
                .orElseGet(() -> env.event().getInteraction()
                        .getMember().orElseThrow()
                        .getDisplayName());

        Function<MessagePaginator.Page, ? extends Mono<MessageCreateSpec>> paginator = page ->
                entityRetriever.getAllModerationActionById(ModerationAction.Type.warn, guildId, targetId)
                        .sort(BaseEntity.timestampComparator)
                        .index().skip(page.getPage() * PER_PAGE).take(PER_PAGE, true)
                        .map(TupleUtils.function((idx, action) -> String.format("**%d.** %s, Модератор: %s" +
                                        action.reason().map(str -> ", Причина: %s").orElse("") + "%n", idx + 1,
                                TimestampFormat.LONG_DATE_TIME.format(InternalId.getTimestamp(action.id())),
                                MessageUtil.getMemberMention(Snowflake.of(action.adminId())), action.reason().orElse(""))))
                        .collect(Collectors.joining())
                        .map(str -> String.format(targetId.equals(author.getId()) ? "Ваши предупреждения"
                                : "Предупреждения пользователя **%s** (%s)",
                                nickname, MessageUtil.getMemberMention(targetId)) + "\n\n" + str)
                        .map(str -> MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .description(str)
                                        .color(env.configuration().discord().embedColor())
                                        .footer(String.format("Страница %s/%s", page.getPage() + 1, page.getPageCount()), null)
                                        .build())
                                .components(page.getItemsCount() > PER_PAGE
                                        ? Possible.of(List.of(ActionRow.of(
                                        page.previousButton(id -> Button.primary(id, "Предыдущая Страница")),
                                        page.nextButton(id -> Button.primary(id, "Следующая Страница")))))
                                        : Possible.absent())
                                .build());

        return entityRetriever.moderationActionCountById(ModerationAction.Type.warn, guildId, targetId)
                .filter(l -> l > 0)
                .switchIfEmpty(messageService.err(env, "Пользователь не имеет предупреждений").then(Mono.never()))
                .flatMap(l -> MessagePaginator.paginate(env, l, PER_PAGE, paginator));
    }
}
