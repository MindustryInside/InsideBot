package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.EmojiData;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_EPHEMERAL;

@InteractionDiscordCommand(name = "starboard", description = "Starboard settings.")
public class StarboardCommand extends OwnerCommand{

    protected StarboardCommand(@Aware List<? extends InteractionOwnerAwareCommand<StarboardCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "enable", description = "Enable starboard.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class StarboardCommandEnable extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandEnable(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            BooleanFunction<String> formatBool = bool ->
                    messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("enable")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.starboard-enable.update",
                                    formatBool.apply(starboardConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                starboardConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.starboard-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "barrier", description = "Configure the starboard barrier.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class StarboardCommandBarrier extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandBarrier(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New message barrier.")
                    .type(ApplicationCommandOptionType.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.barrier.current",
                                    starboardConfig.lowerStarBarrier()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.negative-number")
                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true)).then(Mono.never()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env.event(), "command.settings.overflow-number")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                starboardConfig.lowerStarBarrier(i);
                                return messageService.text(env.event(), "command.settings.barrier.update", i)
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "emojis", description = "Configure starboard emojis.",
            type = ApplicationCommandOptionType.SUB_COMMAND_GROUP)
    public static class StarboardCommandEmojis extends SubGroupOwnerCommand<StarboardCommand>{

        private static final Pattern unicode = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", Pattern.UNICODE_CHARACTER_CLASS);

        protected StarboardCommandEmojis(@Aware StarboardCommand owner,
                                         @Aware List<? extends InteractionOwnerAwareCommand<StarboardCommandEmojis>> subcommands){
            super(owner, subcommands);
        }

        @InteractionDiscordCommand(name = "help", description = "Get a help.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class StarboardCommandEmojisHelp extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisHelp(@Aware StarboardCommandEmojis owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                Function<List<EmojiData>, String> formatEmojis = emojis -> {
                    StringBuilder builder = new StringBuilder();
                    int lastnceil = 0;
                    for(EmojiData data : emojis){
                        builder.append(lastnceil).append("..").append(lastnceil + 5);
                        builder.append(" - ");
                        builder.append(DiscordUtil.getEmojiString(data));
                        builder.append("\n");
                        lastnceil += 5;
                    }
                    return builder.toString();
                };

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> messageService.text(env.event(), "command.settings.emojis.current",
                                formatEmojis.apply(starboardConfig.emojis())));
            }
        }

        @InteractionDiscordCommand(name = "add", description = "Add emoji(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class StarboardCommandEmojisAdd extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisAdd(@Aware StarboardCommandEmojis owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Target emoji(s).")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> {
                            String value = env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .orElseThrow(IllegalStateException::new);

                            String[] text = value.split("(\\s+)?,(\\s+)?");
                            return Flux.fromArray(text).flatMap(str -> env.getClient().getGuildEmojis(guildId)
                                            .filter(emoji -> emoji.asFormat().equals(str) || emoji.getName().equals(str) ||
                                                    emoji.getId().asString().equals(str))
                                            .map(GuildEmoji::getData)
                                            .switchIfEmpty(Mono.just(str)
                                                    .filter(s -> unicode.matcher(s).find())
                                                    .map(s -> EmojiData.builder()
                                                            .name(s)
                                                            .build())))
                                    .collectList()
                                    .flatMap(list -> {
                                        List<EmojiData> emojis = starboardConfig.emojis();

                                        if(list.size() + emojis.size() > 20){
                                            return messageService.err(env.event(), "command.settings.emojis.limit")
                                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                        }

                                        emojis.addAll(list);
                                        return messageService.text(env.event(), "command.settings.added", list.stream()
                                                        .map(DiscordUtil::getEmojiString)
                                                        .collect(Collectors.joining(", ")))
                                                .and(entityRetriever.save(starboardConfig));
                                    });
                        });
            }
        }

        @InteractionDiscordCommand(name = "remove", description = "Remove emoji(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class StarboardCommandEmojisRemove extends OwnerAwareCommand<StarboardCommandEmojis>{

            private static final Pattern indexModePattern = Pattern.compile("^(#\\d+)$");

            protected StarboardCommandEmojisRemove(@Aware StarboardCommandEmojis owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Target emoji(s).")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> {
                            String value = env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .orElseThrow(IllegalStateException::new);

                            List<EmojiData> emojis = starboardConfig.emojis();

                            if(indexModePattern.matcher(value).matches()){ // index mode
                                String str = value.substring(1);
                                if(!MessageUtil.canParseInt(str)){
                                    return messageService.err(env.event(), "command.settings.emojis.overflow-index")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                int idx = Strings.parseInt(str) - 1; // Counting the index from 1
                                if(idx < 0 || idx >= emojis.size()){
                                    return messageService.err(env.event(), "command.settings.emojis.index-out-of-bounds")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                EmojiData data = emojis.remove(idx);
                                return messageService.text(env.event(), "command.settings.removed",
                                        DiscordUtil.getEmojiString(data));
                            }

                            String[] text = value.split("(\\s+)?,(\\s+)?");
                            return Flux.fromArray(text).flatMap(str -> env.getClient().getGuildEmojis(guildId)
                                            .filter(emoji -> emoji.asFormat().equals(str) || emoji.getName().equals(str) ||
                                                    emoji.getId().asString().equals(str))
                                            .map(GuildEmoji::getData)
                                            .switchIfEmpty(Mono.just(str)
                                                    .filter(s -> unicode.matcher(s).find())
                                                    .map(s -> EmojiData.builder()
                                                            .name(s)
                                                            .build())))
                                    .collectList()
                                    .flatMap(list -> {
                                        var tmp = new ArrayList<>(emojis);
                                        tmp.removeAll(list);

                                        if(tmp.size() < 1){
                                            return messageService.err(env.event(), "command.settings.emojis.no-emojis")
                                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                        }

                                        emojis.removeAll(list);
                                        return messageService.text(env.event(), "command.settings.removed", list.stream()
                                                        .map(DiscordUtil::getEmojiString)
                                                        .collect(Collectors.joining(", ")))
                                                .and(entityRetriever.save(starboardConfig));
                                    });
                        });
            }
        }

        @InteractionDiscordCommand(name = "clear", description = "Remove all emojis.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class StarboardCommandEmojisClear extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisClear(@Aware StarboardCommandEmojis owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> {
                            List<EmojiData> emojis = starboardConfig.emojis();
                            emojis.clear();
                            return messageService.text(env.event(), "command.settings.emojis.clear")
                                    .and(entityRetriever.save(starboardConfig));
                        });
            }
        }
    }

    @InteractionDiscordCommand(name = "channel", description = "Configure starboard channel.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class StarboardCommandChannel extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandChannel(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New starboard channel.")
                    .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.starboard-channel.current",
                                            starboardConfig.starboardChannelId().map(DiscordUtil::getChannelMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(channelId -> {
                                starboardConfig.starboardChannelId(channelId);
                                return messageService.text(env.event(), "command.settings.starboard-channel.update",
                                                DiscordUtil.getChannelMention(channelId))
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }
}
