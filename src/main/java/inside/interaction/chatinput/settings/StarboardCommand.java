package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.EmojiData;
import inside.data.EntityRetriever;
import inside.data.entity.StarboardConfig;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;

import static inside.interaction.chatinput.settings.ReactionRolesCommand.emojiPattern;
import static reactor.function.TupleUtils.function;

@ChatInputCommand(name = "starboard", description = "Настройки звёздной доски.", permissions = PermissionCategory.OWNER)
public class StarboardCommand extends ConfigOwnerCommand {

    public StarboardCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new EnableSubcommand(this));
        addSubcommand(new ThresholdSubcommand(this));
        addSubcommand(new ChannelSubcommand(this));
        addSubcommand(new SelfStarringSubcommand(this));
        addSubcommand(new EmojisSubcommandGroup(this));
    }

    @Subcommand(name = "enable", description = "Включить ведение звёздной доски.")
    protected static class EnableSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected EnableSubcommand(StarboardCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новое состояние.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(messageService.err(env, "commands.starboard.enable.unconfigured").then(Mono.never()))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "commands.starboard.enable.format",
                                    messageService.getBool(env.context(), "commands.starboard.enable", config.enabled())).then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "commands.starboard.enable.format",
                                            messageService.getBool(env.context(), "commands.starboard.enable", state))
                                    .and(owner.entityRetriever.save(config.withEnabled(state)))));
        }
    }

    @Subcommand(name = "self-starring", description = "Настроить учёт собственной реакции.")
    protected static class SelfStarringSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected SelfStarringSubcommand(StarboardCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новое состояние.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "commands.starboard.self-starring.format",
                                    messageService.getBool(env.context(), "commands.starboard.self-starring", config.selfStarring())).then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "commands.starboard.self-starring.format",
                                    messageService.getBool(env.context(), "commands.starboard.self-starring", state))
                                    .and(owner.entityRetriever.save(config.withSelfStarring(state)))));
        }
    }

    @Subcommand(name = "threshold", description = "Настроить порог реакция для добавления звёздной доски.")
    protected static class ThresholdSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected ThresholdSubcommand(StarboardCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новый порог.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                    .minValue(0d)
                    .maxValue((double) Integer.MAX_VALUE));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong)
                                    .map(Math::toIntExact))
                            .switchIfEmpty(messageService.text(env, "commands.starboard.threshold.current", config.threshold() == -1
                                    ? messageService.get(env.context(), "commands.starboard.threshold.absent")
                                    : config.threshold()).then(Mono.never()))
                            .flatMap(threshold -> messageService.text(env, "commands.starboard.threshold.update", threshold)
                                    .and(owner.entityRetriever.save(config.withThreshold(threshold)))));
        }
    }

    @Subcommand(name = "channel", description = "Настроить канал для ведения звёздной доски.")
    protected static class ChannelSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected ChannelSubcommand(StarboardCommand owner) {
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Новый текстовый канал.")
                    .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                    .channelTypes(Channel.Type.GUILD_TEXT.getValue()));
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env, "commands.starboard.channel.current", config.starboardChannelId() == -1
                                    ? messageService.get(env.context(), "commands.starboard.channel.absent")
                                    : MessageUtil.getChannelMention(config.starboardChannelId())).then(Mono.never()))
                            .flatMap(channelId -> messageService.text(env, "commands.starboard.channel.update",
                                    MessageUtil.getChannelMention(channelId))
                                    .and(owner.entityRetriever.save(config.withStarboardChannelId(channelId.asLong())))));
        }
    }

    @SubcommandGroup(name = "emojis", description = "Настроить учитываемые в подсчёте реакции.")
    protected static class EmojisSubcommandGroup extends ConfigOwnerCommand {

        protected EmojisSubcommandGroup(StarboardCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand(name = "add", description = "Добавить эмодзи в список.")
        protected static class AddSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected AddSubcommand(EmojisSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Идентификатор/название эмодзи или юникод символ.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                var emojistr = env.getOption("value")
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

                return fetchEmoji.zipWith(owner.entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId)))
                        .map(function((emoji, config) -> {
                            var set = new HashSet<>(config.emojis());
                            boolean add = set.add(emoji);
                            if (!add) {
                                return messageService.err(env, "commands.starboard.emoji.add.already-exists");
                            }

                            return messageService.text(env, "commands.starboard.emoji.add.success",
                                            MessageUtil.getEmojiString(emoji))
                                    .and(owner.entityRetriever.save(config.withEmojis(set)));
                        }));
            }
        }

        @Subcommand(name = "remove", description = "Удалить эмодзи из списка.")
        protected static class RemoveSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected RemoveSubcommand(EmojisSubcommandGroup owner) {
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Идентификатор/название эмодзи или юникод символ.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                var emojistr = env.getOption("value")
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

                return fetchEmoji.zipWith(owner.entityRetriever.getStarboardConfigById(guildId)
                                .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId)))
                        .map(function((emoji, config) -> {
                            var set = new HashSet<>(config.emojis());
                            boolean remove = set.remove(emoji);
                            if (!remove) {
                                return messageService.err(env, "commands.starboard.emoji.remove.unknown");
                            }

                            return messageService.text(env, "commands.starboard.emoji.remove.success", MessageUtil.getEmojiString(emoji))
                                    .and(owner.entityRetriever.save(config.withEmojis(set)));
                        }));
            }
        }

        @Subcommand(name = "clear", description = "Отчистить список эмодзи.")
        protected static class ClearSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected ClearSubcommand(EmojisSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                        .flatMap(config -> config.emojis().isEmpty()
                                ? messageService.err(env, "commands.starboard.emoji.list.empty")
                                : messageService.text(env, "commands.starboard.emoji.clear.success")
                                .and(owner.entityRetriever.save(config.withEmojis(List.of()))));
            }
        }

        @Subcommand(name = "list", description = "Отобразить список эмодзи.")
        protected static class ListSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected ListSubcommand(EmojisSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                return owner.entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                        .flatMap(config -> config.emojis().isEmpty()
                                ? messageService.err(env, "commands.starboard.emoji.list.empty")
                                : messageService.infoTitled(env, "commands.starboard.emoji.list.title", formatEmojis(config)));
            }

            private static String formatEmojis(StarboardConfig config){
                StringBuilder builder = new StringBuilder();
                int lastnceil = 0;
                boolean first = true;
                int d = config.threshold();
                for(EmojiData data : config.emojis()){
                    builder.append(lastnceil).append("..").append(lastnceil + d);
                    builder.append(" - ");
                    builder.append(MessageUtil.getEmojiString(data));
                    builder.append('\n');
                    lastnceil += d;
                    if(first){
                        // TODO: customize period
                        d = 5;
                        first = false;
                    }
                }
                return builder.toString();
            }
        }
    }
}
