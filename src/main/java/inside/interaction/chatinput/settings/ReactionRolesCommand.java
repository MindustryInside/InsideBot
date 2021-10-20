package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.EmojiData;
import inside.annotation.Aware;
import inside.data.entity.EmojiDispenser;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@ChatInputCommand(name = "reaction-roles", description = "Configure reaction roles.")
public class ReactionRolesCommand extends OwnerCommand{

    protected ReactionRolesCommand(@Aware List<? extends InteractionOwnerAwareCommand<ReactionRolesCommand>> subcommands){
        super(subcommands);
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env){
        return super.execute(env);
    }

    private static String format(EmojiDispenser e){
        return String.format("%s -> %s (%s)\n",
                e.getMessageId().asString(), DiscordUtil.getRoleMention(e.getRoleId()),
                DiscordUtil.getEmojiString(e.getEmoji()));
    }

    @Subcommand(name = "list", description = "Display current reaction roles.")
    public static class ReactionRolesCommandList extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandList(@Aware ReactionRolesCommand owner){
            super(owner);
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getAllEmojiDispenserInGuild(guildId)
                    .switchIfEmpty(messageService.err(env, "command.settings.reaction-roles.absents").then(Mono.never()))
                    .map(ReactionRolesCommand::format)
                    .collect(Collectors.joining())
                    .flatMap(str -> messageService.text(env,
                            "command.settings.reaction-roles.current",
                            str.isBlank() ? "command.settings.absents" : str));
        }
    }

    @Subcommand(name = "add", description = "Add reaction role.")
    public static class ReactionRolesCommandAdd extends OwnerAwareCommand<ReactionRolesCommand>{

        private static final int MAX_REACTION_ROLE_COUNT = 20;

        protected ReactionRolesCommandAdd(@Aware ReactionRolesCommand owner){
            super(owner);

            addOption(builder -> builder.name("emoji")
                    .description("Unicode or custom emoji.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("message-id")
                    .description("Listening message ID.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Dispensed role.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            //TODO: USE!
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            String emojistr = env.getOption("emoji")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow();

            Mono<EmojiData> fetchEmoji = env.getClient().getGuildEmojis(guildId)
                    .filter(emoji -> emoji.asFormat().equals(emojistr) ||
                            emoji.getName().equals(emojistr) ||
                            emoji.getId().asString().equals(emojistr))
                    .map(GuildEmoji::getData)
                    .defaultIfEmpty(EmojiData.builder()
                            .name(emojistr)
                            .build())
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

            if(messageId == null){
                return messageService.text(env, "command.settings.reaction-roles.incorrect-message-id");
            }

            return fetchEmoji.filterWhen(ignored -> entityRetriever.getEmojiDispenserCountInGuild(guildId)
                            .map(l -> l < MAX_REACTION_ROLE_COUNT))
                    .switchIfEmpty(messageService.text(env,
                            "command.settings.reaction-roles.limit").then(Mono.empty()))
                    .flatMap(emoji -> entityRetriever.createEmojiDispenser(guildId, messageId, roleId, emoji)
                            .flatMap(e -> messageService.text(env, "command.settings.added", format(e))));
        }
    }

    @Subcommand(name = "remove", description = "Remove reaction role.")
    public static class ReactionRolesCommandRemove extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandRemove(@Aware ReactionRolesCommand owner){
            super(owner);

            addOption(builder -> builder.name("message-id")
                    .description("Listening message ID.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Dispensed role.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            //TODO: USE!
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

            if(messageId == null){
                return messageService.text(env, "command.settings.reaction-roles.incorrect-message-id");
            }

            return entityRetriever.getEmojiDispenserById(messageId, roleId)
                    .switchIfEmpty(messageService.text(env,
                            "command.settings.reaction-roles.not-found").then(Mono.empty()))
                    .flatMap(e -> messageService.text(env, "command.settings.removed", format(e))
                            .and(entityRetriever.delete(e)));
        }
    }

    @Subcommand(name = "clear", description = "Remove all reaction roles.")
    public static class ReactionRolesCommandClear extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandClear(@Aware ReactionRolesCommand owner){
            super(owner);
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getEmojiDispenserCountInGuild(guildId)
                    .flatMap(l -> entityRetriever.deleteAllEmojiDispenserInGuild(guildId)
                            .then(messageService.text(env,
                                    l == 0 ? "command.settings.removed-nothing" : "command.settings.reaction-roles.clear")));
        }
    }
}
