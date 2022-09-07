package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.EmojiData;
import inside.data.EntityRetriever;
import inside.data.entity.EmojiDataWithPeriod;
import inside.data.entity.StarboardConfig;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Option;
import inside.interaction.annotation.Subcommand;
import inside.interaction.annotation.SubcommandGroup;
import inside.interaction.chatinput.InteractionSubcommand;
import inside.interaction.chatinput.InteractionSubcommandGroup;
import inside.service.MessageService;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;

import static inside.interaction.chatinput.settings.ReactionRolesCommand.emojiPattern;
import static reactor.function.TupleUtils.function;

@ChatInputCommand(value = "starboard")// = "Настройки звёздной доски.", permissions = PermissionCategory.ADMIN)
public class StarboardCommand extends InteractionSubcommandGroup {

    public StarboardCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService, entityRetriever);

        addSubcommand(new EnableSubcommand(this));
        addSubcommand(new ThresholdSubcommand(this));
        addSubcommand(new ChannelSubcommand(this));
        addSubcommand(new SelfStarringSubcommand(this));
        addSubcommand(new EmojisSubcommandGroup(this));
    }

    @Subcommand("enable")// = "Включить ведение звёздной доски.")
    @Option(name = "value", type = Type.BOOLEAN)
    protected static class EnableSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected EnableSubcommand(StarboardCommand owner) {
            super(owner);
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .filter(config -> !config.emojis().isEmpty() && config.threshold() != -1 &&
                            config.starboardChannelId() != -1)
                    .switchIfEmpty(messageService.err(env, "Сначала измените настройки звёздной доски").then(Mono.never()))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .filter(s -> s != config.enabled())
                            .switchIfEmpty(messageService.text(env, "Ведение звёздной доски: **%s**",
                                    config.enabled() ? "включено" : "выключено").then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "Ведение звёздной доски: **%s**",
                                            state ? "включено" : "выключено")
                                    .and(owner.entityRetriever.save(config.withEnabled(state)))));
        }
    }

    @Subcommand("self-starring")// = "Настроить учёт собственной реакции.")
    @Option(name = "value", type = Type.BOOLEAN)
    protected static class SelfStarringSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected SelfStarringSubcommand(StarboardCommand owner) {
            super(owner);
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .filter(s -> s != config.selfStarring())
                            .switchIfEmpty(messageService.text(env, "Учёт собственной реакции: **%s**",
                                            config.selfStarring() ? "включен" : "выключен")
                                    .then(Mono.never()))
                            .flatMap(state -> messageService.text(env, "Учёт собственной реакции: **%s**",
                                            state ? "включен" : "выключен")
                                    .and(owner.entityRetriever.save(config.withSelfStarring(state)))));
        }
    }

    @Subcommand("threshold")// = "Настроить порог реакция для добавления звёздной доски.")
    @Option(name = "value", type = Type.INTEGER, minValue = 0, maxValue = Integer.MAX_VALUE)
    protected static class ThresholdSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected ThresholdSubcommand(StarboardCommand owner) {
            super(owner);
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
                            .switchIfEmpty(messageService.text(env, "Текущий порог реакций: **%s**", config.threshold() == -1
                                    ? "не установлен" : config.threshold()).then(Mono.never()))
                            .flatMap(threshold -> messageService.text(env, "Порог реакций обновлен: **%s**", threshold)
                                    .and(owner.entityRetriever.save(config.withThreshold(threshold)))));
        }
    }

    @Subcommand("channel")// = "Настроить канал для ведения звёздной доски.")
    @Option(name = "value", type = Type.CHANNEL, channelTypes = Channel.Type.GUILD_TEXT)
    protected static class ChannelSubcommand extends InteractionSubcommand<StarboardCommand> {

        protected ChannelSubcommand(StarboardCommand owner) {
            super(owner);
        }

        @Override
        public Publisher<?> execute(ChatInputInteractionEnvironment env) {

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return owner.entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId))
                    .flatMap(config -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env, "Текущий канал для ведения звёздной доски: **%s**",
                                    config.starboardChannelId() == -1 ? "не установлен"
                                            : MessageUtil.getChannelMention(config.starboardChannelId())).then(Mono.never()))
                            .filter(s -> s.asLong() != config.starboardChannelId())
                            .switchIfEmpty(messageService.err(env, "Канал для ведения здвёздной доски не изменён").then(Mono.never()))
                            .flatMap(channelId -> messageService.text(env, "Канал для ведения доски обновлён: %s",
                                            MessageUtil.getChannelMention(channelId))
                                    .and(owner.entityRetriever.save(config.withStarboardChannelId(channelId.asLong())))));
        }
    }

    @SubcommandGroup(value = "emojis")// = "Настроить учитываемые в подсчёте реакции.")
    protected static class EmojisSubcommandGroup extends InteractionSubcommandGroup {

        protected EmojisSubcommandGroup(StarboardCommand owner) {
            super(owner.messageService, owner.entityRetriever);

            addSubcommand(new AddSubcommand(this));
            addSubcommand(new RemoveSubcommand(this));
            addSubcommand(new ClearSubcommand(this));
            addSubcommand(new ListSubcommand(this));
        }

        @Subcommand("add")// = "Добавить эмодзи в список.")
        @Option(name = "value", type = Type.STRING, required = true)
        @Option(name = "period", type = Type.INTEGER, minValue = 1, maxValue = Integer.MAX_VALUE)
        protected static class AddSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected AddSubcommand(EmojisSubcommandGroup owner) {
                super(owner);
            }

            @Override
            public Publisher<?> execute(ChatInputInteractionEnvironment env) {

                Snowflake guildId = env.event().getInteraction()
                        .getGuildId().orElseThrow();

                var emojistr = env.getOption("value")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElseThrow();

                int period = env.getOption("period")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asLong)
                        .map(Math::toIntExact)
                        .orElse(EmojiDataWithPeriod.DEFAULT_PERIOD);

                Mono<EmojiData> fetchEmoji = env.event().getClient().getGuildEmojis(guildId)
                        .filter(emoji -> emoji.asFormat().equals(emojistr) ||
                                emoji.getName().equals(emojistr) ||
                                emoji.getId().asString().equals(emojistr))
                        .map(GuildEmoji::getData)
                        .switchIfEmpty(Mono.defer(() -> {
                            Matcher mtch = emojiPattern.matcher(emojistr);
                            if (!mtch.matches()) {
                                return messageService.err(env, "Неправильный формат эмодзи").then(Mono.empty());
                            }
                            return Mono.just(EmojiData.builder()
                                    .name(emojistr)
                                    .build());
                        }))
                        .next();

                return fetchEmoji.zipWith(owner.entityRetriever.getStarboardConfigById(guildId)
                                .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId)))
                        .flatMap(function((emoji, config) -> {
                            var entry = EmojiDataWithPeriod.builder()
                                    .emoji(emoji)
                                    .period(period)
                                    .build();

                            var set = new HashSet<>(config.emojis());
                            boolean add = set.add(entry);
                            if (!add) {
                                return messageService.err(env, "Такая реакция уже находится в списке.");
                            }

                            return messageService.text(env, "Реакция успешно добавлена в список: **%s** с порогом **%s**",
                                            MessageUtil.getEmojiString(emoji), period)
                                    .and(owner.entityRetriever.save(config.withEmojis(set)));
                        }));
            }
        }

        @Subcommand("remove")// = "Удалить эмодзи из списка.")
        @Option(name = "value", type = Type.STRING, required = true)
        protected static class RemoveSubcommand extends InteractionSubcommand<EmojisSubcommandGroup> {

            protected RemoveSubcommand(EmojisSubcommandGroup owner) {
                super(owner);
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
                                return messageService.err(env, "Неправильный формат эмодзи").then(Mono.empty());
                            }
                            return Mono.just(EmojiData.builder()
                                    .name(emojistr)
                                    .build());
                        }))
                        .next();

                return fetchEmoji.zipWith(owner.entityRetriever.getStarboardConfigById(guildId)
                                .switchIfEmpty(owner.entityRetriever.createStarboardConfig(guildId)))
                        .flatMap(function((emoji, config) -> {
                            var entry = EmojiDataWithPeriod.builder()
                                    .emoji(emoji)
                                    .build();

                            var set = new HashSet<>(config.emojis());
                            boolean remove = set.remove(entry);
                            if (!remove) {
                                return messageService.err(env, "Такой реакции нет в списке.");
                            }

                            return messageService.text(env, "Реакция успешно удалена из списка: **%s**",
                                            MessageUtil.getEmojiString(emoji))
                                    .and(owner.entityRetriever.save(config.withEmojis(set)));
                        }));
            }
        }

        @Subcommand(value = "clear")// = "Отчистить список эмодзи.")
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
                                ? messageService.err(env, "Список эмодзи пуст")
                                : messageService.text(env, "Список эмодзи очищен")
                                .and(owner.entityRetriever.save(config.withEmojis(List.of()))));
            }
        }

        @Subcommand(value = "list")// = "Отобразить список эмодзи.")
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
                        .filter(t -> t.threshold() != -1)
                        .switchIfEmpty(messageService.err(env, "Порог реакций не настроен").then(Mono.never()))
                        .flatMap(config -> config.emojis().isEmpty()
                                ? messageService.err(env, "Список эмодзи пуст")
                                : messageService.infoTitled(env, "Текущий список эмодзи", formatEmojis(config)));
            }

            private static String formatEmojis(StarboardConfig config) {
                StringBuilder builder = new StringBuilder();
                var emojis = new ArrayList<>(config.emojis());
                emojis.sort(Comparator.comparingInt(EmojiDataWithPeriod::period));

                for (int i = 0, t = config.threshold(); i < emojis.size(); i++) {
                    EmojiDataWithPeriod data = emojis.get(i);
                    builder.append("**").append(t).append("..").append(t += data.period()).append("**");
                    builder.append(" - ");
                    builder.append(MessageUtil.getEmojiString(data.emoji()));
                    builder.append('\n');
                }
                return builder.toString();
            }
        }
    }
}
