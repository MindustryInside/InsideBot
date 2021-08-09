package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.EmojiData;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.data.entity.EmojiDispenser;
import inside.interaction.*;
import inside.util.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@InteractionDiscordCommand(name = "reaction-roles", description = "Configure reaction roles.")
public class ReactionRolesCommand extends OwnerCommand{

    protected ReactionRolesCommand(@Aware List<? extends InteractionOwnerAwareCommand<ReactionRolesCommand>> subcommands){
        super(subcommands);
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        return super.execute(env);
    }

    private static String format(EmojiDispenser e){
        return String.format("%s -> %s (%s)\n",
                e.getMessageId().asString(), DiscordUtil.getRoleMention(e.getRoleId()),
                DiscordUtil.getEmojiString(e.getEmoji()));
    }

    @InteractionDiscordCommand(name = "help", description = "Get a help.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ReactionRolesCommandHelp extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandHelp(@Aware ReactionRolesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAllEmojiDispenserInGuild(guildId)
                    //TODO: "empty" check
                    .map(ReactionRolesCommand::format)
                    .collect(Collectors.joining())
                    .flatMap(str -> messageService.text(env.event(),
                            "command.settings.reaction-roles.current", str));
        }
    }

    @InteractionDiscordCommand(name = "add", description = "Add reaction role.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ReactionRolesCommandAdd extends OwnerAwareCommand<ReactionRolesCommand>{

        private static final int MAX_REACTION_ROLE_COUNT = 20;

        protected ReactionRolesCommandAdd(@Aware ReactionRolesCommand owner){
            super(owner);

            addOption(builder -> builder
                    .name("emoji")
                    .description("Target emoji.")
                    .required(true)
                    .type(ApplicationCommandOptionType.STRING.getValue()));

            addOption(builder -> builder.name("message-id")
                    .description("Target message id.")
                    .required(true)
                    .type(ApplicationCommandOptionType.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Target role.")
                    .required(true)
                    .type(ApplicationCommandOptionType.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            //TODO: USE!
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            String emojistr = env.getOption("emoji")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(IllegalStateException::new);

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
                    .orElseThrow(IllegalStateException::new);

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if(messageId == null){
                return messageService.text(env.event(), "command.settings.reaction-roles.incorrect-message-id");
            }

            return fetchEmoji.filterWhen(ignored -> entityRetriever.getEmojiDispenserCountInGuild(guildId)
                            .map(l -> l < MAX_REACTION_ROLE_COUNT))
                    .switchIfEmpty(messageService.text(env.event(),
                            "command.settings.reaction-roles.limit").then(Mono.empty()))
                    .flatMap(emoji -> entityRetriever.createEmojiDispenser(guildId, messageId, roleId, emoji)
                            .flatMap(e -> messageService.text(env.event(), "command.settings.added", format(e))));
        }
    }

    @InteractionDiscordCommand(name = "remove", description = "Remove reaction role.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ReactionRolesCommandRemove extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandRemove(@Aware ReactionRolesCommand owner){
            super(owner);

            addOption(builder -> builder.name("message-id")
                    .description("Target message id.")
                    .required(true)
                    .type(ApplicationCommandOptionType.STRING.getValue()));

            addOption(builder -> builder.name("role")
                    .description("Target role.")
                    .required(true)
                    .type(ApplicationCommandOptionType.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            //TODO: USE!
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            Snowflake roleId = env.getOption("role")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asSnowflake)
                    .orElseThrow(IllegalStateException::new);

            Snowflake messageId = env.getOption("message-id")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(MessageUtil::parseId)
                    .orElse(null);

            if(messageId == null){
                return messageService.text(env.event(), "command.settings.reaction-roles.incorrect-message-id");
            }

            return entityRetriever.getEmojiDispenserById(messageId, roleId)
                    .switchIfEmpty(messageService.text(env.event(),
                            "command.settings.reaction-roles.not-found").then(Mono.empty()))
                    .flatMap(e -> messageService.text(env.event(), "command.settings.removed", format(e))
                            .and(entityRetriever.delete(e)));
        }
    }

    @InteractionDiscordCommand(name = "clear", description = "Remove all reaction roles.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ReactionRolesCommandClear extends OwnerAwareCommand<ReactionRolesCommand>{

        protected ReactionRolesCommandClear(@Aware ReactionRolesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.deleteAllEmojiDispenserInGuild(guildId)
                    .then(messageService.text(env.event(), "command.settings.reaction-roles.clear"));
        }
    }
}