package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.EmojiData;
import inside.data.EntityRetriever;
import inside.data.entity.ReactionRole;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static inside.data.entity.ReactionRole.MAX_PER_MESSAGE;
import static reactor.bool.BooleanUtils.not;

@ChatInputCommand(name = "reaction-roles", description = "Настройки реакций-ролей.")
public class ReactionRolesCommand extends ConfigOwnerCommand {

    public static Pattern emojiPattern = Pattern.compile("^\\p{So}$");

    public ReactionRolesCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new ListSubcommand(this));
        addSubcommand(new AddSubcommand(this));
        addSubcommand(new RemoveSubcommand(this));
        addSubcommand(new ClearSubcommand(this));
    }

    static String format(ReactionRole e) {
        return String.format("%s -> %s (%s)\n",
                e.messageId(), MessageUtil.getRoleMention(e.roleId()),
                MessageUtil.getEmojiString(e.emoji()));
    }

    @Subcommand(name = "list", description = "Отобразить текущий список реакций-ролей для указанного сообщения.")
    protected static class ListSubcommand extends InteractionSubcommand<ReactionRolesCommand> {

        protected ListSubcommand(ReactionRolesCommand owner) {
            super(owner);

            addOption(builder -> builder.name("message-id")
                    .description("Идентификатор ассоциируемого сообщения.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if (messageId == null) {
                return messageService.err(env, "commands.reaction-roles.incorrect-message-id");
            }

            return owner.entityRetriever.getAllReactionRolesById(guildId, messageId)
                    .switchIfEmpty(messageService.err(env, "commands.reaction-roles.list.empty").then(Mono.never()))
                    .map(ReactionRolesCommand::format)
                    .collect(Collectors.joining())
                    .flatMap(str -> messageService.infoTitled(env, "commands.reaction-roles.list.title", str));
        }
    }

    @Subcommand(name = "add", description = "Добавить новую реакцию-роль к сообщению.")
    protected static class AddSubcommand extends InteractionSubcommand<ReactionRolesCommand> {

        protected AddSubcommand(ReactionRolesCommand owner) {
            super(owner);

            addOption(builder -> builder.name("emoji")
                    .description("Идентификатор/имя реакции или юникод символ.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("message-id")
                    .description("Идентификатор ассоциируемого сообщения.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Выдаваемая при нажатии роль.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            String emojistr = env.getOption("emoji")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();

            Mono<EmojiData> fetchEmoji = env.event().getClient().getGuildEmojis(guildId)
                    .filter(emoji -> emoji.asFormat().equals(emojistr) ||
                            emoji.getName().equals(emojistr) ||
                            emoji.getId().asString().equals(emojistr))
                    .map(GuildEmoji::getData)
                    .switchIfEmpty(Mono.defer(() -> {
                        Matcher mtch = emojiPattern.matcher(emojistr);
                        if (!mtch.matches()) {
                            return messageService.err(env, "commands.common.emoji-invalid").then(Mono.empty());
                        }
                        return Mono.just(EmojiData.builder()
                                .name(emojistr)
                                .build());
                    }))
                    .next();

            Snowflake roleId = env.getOption("role")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                    .orElseThrow();

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if (messageId == null) {
                return messageService.err(env, "commands.reaction-roles.incorrect-message-id");
            }

            return fetchEmoji.filterWhen(ignored -> owner.entityRetriever.reactionRolesCountById(guildId, messageId)
                            .map(l -> l < MAX_PER_MESSAGE))
                    .switchIfEmpty(messageService.err(env, "commands.reaction-roles.add.limit", MAX_PER_MESSAGE).then(Mono.empty()))
                    .filterWhen(e -> not(owner.entityRetriever.getReactionRoleById(guildId, messageId, roleId).hasElement()))
                    .switchIfEmpty(messageService.err(env, "commands.reaction-roles.add.already-exists").then(Mono.empty()))
                    .map(emoji -> ReactionRole.builder()
                            .guildId(guildId.asLong())
                            .messageId(messageId.asLong())
                            .roleId(roleId.asLong())
                            .emoji(emoji)
                            .build())
                    .flatMap(reactionRole -> messageService.text(env, "commands.reaction-roles.add.success", format(reactionRole))
                            .and(owner.entityRetriever.save(reactionRole)));
        }
    }

    @Subcommand(name = "remove", description = "Удалить реакцию-роль с сообщения.")
    protected static class RemoveSubcommand extends InteractionSubcommand<ReactionRolesCommand> {

        protected RemoveSubcommand(ReactionRolesCommand owner) {
            super(owner);

            addOption(builder -> builder.name("message-id")
                    .description("Идентификатор сообщения, с которого нужно удалить реакцию-роль.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Получаемая при нажатии роль.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Snowflake roleId = env.getOption("role")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                    .orElseThrow();

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if (messageId == null) {
                return messageService.err(env, "commands.reaction-roles.incorrect-message-id");
            }

            return owner.entityRetriever.getReactionRoleById(guildId, messageId, roleId)
                    .switchIfEmpty(messageService.err(env, "commands.reaction-roles.remove.unknown").then(Mono.empty()))
                    .flatMap(e -> messageService.text(env, "commands.reaction-roles.remove.success", format(e))
                            .and(owner.entityRetriever.delete(e)));
        }
    }

    @Subcommand(name = "clear", description = "Удалить все реакции-роли с сообщения.")
    protected static class ClearSubcommand extends InteractionSubcommand<ReactionRolesCommand> {

        protected ClearSubcommand(ReactionRolesCommand owner) {
            super(owner);

            addOption(builder -> builder.name("message-id")
                    .description("Идентификатор сообщения, с которого нужно убрать все реакции-роли.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if (messageId == null) {
                return messageService.err(env, "commands.reaction-roles.incorrect-message-id");
            }

            return owner.entityRetriever.reactionRolesCountById(guildId, messageId)
                    .filter(l -> l > 0)
                    .switchIfEmpty(messageService.text(env, "commands.reaction-roles.list.empty").then(Mono.never()))
                    .flatMap(l -> messageService.text(env, "commands.reaction-roles.clear.success")
                            .and(owner.entityRetriever.deleteAllReactionRolesById(guildId, messageId)));
        }
    }
}
