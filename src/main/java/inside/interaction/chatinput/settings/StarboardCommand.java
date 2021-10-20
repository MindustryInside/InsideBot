package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.discordjson.json.EmojiData;
import inside.annotation.Aware;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ChatInputCommand(name = "starboard", description = "Starboard settings.")
public class StarboardCommand extends OwnerCommand{

    protected StarboardCommand(@Aware List<? extends InteractionOwnerAwareCommand<StarboardCommand>> subcommands){
        super(subcommands);
    }

    @Subcommand(name = "enable", description = "Enable starboard.")
    public static class StarboardCommandEnable extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandEnable(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            BooleanFunction<String> formatBool = bool ->
                    messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "command.settings.starboard-enable.update",
                                    formatBool.apply(starboardConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                starboardConfig.setEnabled(bool);
                                return messageService.text(env, "command.settings.starboard-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }

    @Subcommand(name = "barrier", description = "Configure the starboard barrier.")
    public static class StarboardCommandBarrier extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandBarrier(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New message barrier.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env, "command.settings.barrier.current",
                                    starboardConfig.getLowerStarBarrier()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env, "command.settings.negative-number").then(Mono.never()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env, "command.settings.overflow-number");
                                }

                                starboardConfig.setLowerStarBarrier(i);
                                return messageService.text(env, "command.settings.barrier.update", i)
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }

    @SubcommandGroup(name = "emojis", description = "Configure starboard emojis.")
    public static class StarboardCommandEmojis extends SubGroupOwnerCommand<StarboardCommand>{

        private static final Pattern unicode = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", Pattern.UNICODE_CHARACTER_CLASS);

        protected StarboardCommandEmojis(@Aware StarboardCommand owner,
                                         @Aware List<? extends InteractionOwnerAwareCommand<StarboardCommandEmojis>> subcommands){
            super(owner, subcommands);
        }

        @Subcommand(name = "list", description = "Display current emoji list.")
        public static class StarboardCommandEmojisHelp extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisHelp(@Aware StarboardCommandEmojis owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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
                        .flatMap(starboardConfig -> messageService.text(env, "command.settings.emojis.current",
                                starboardConfig.getEmojis().isEmpty()
                                        ? "command.settings.absents"
                                        : formatEmojis.apply(starboardConfig.getEmojis())));
            }
        }

        @Subcommand(name = "add", description = "Add emoji(s).")
        public static class StarboardCommandEmojisAdd extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisAdd(@Aware StarboardCommandEmojis owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("New emoji(s).")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> {
                            String value = env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .orElseThrow();

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
                                        List<EmojiData> emojis = starboardConfig.getEmojis();

                                        if(list.size() + emojis.size() > 20){
                                            return messageService.err(env, "command.settings.emojis.limit");
                                        }

                                        emojis.addAll(list);
                                        String str = list.stream()
                                                .map(DiscordUtil::getEmojiString)
                                                .collect(Collectors.joining(", "));

                                        return messageService.text(env, "command.settings.added"
                                                + (str.isBlank() ? "-nothing" : ""), str)
                                                .and(entityRetriever.save(starboardConfig));
                                    });
                        });
            }
        }

        @Subcommand(name = "remove", description = "Remove emoji(s).")
        public static class StarboardCommandEmojisRemove extends OwnerAwareCommand<StarboardCommandEmojis>{

            private static final Pattern indexModePattern = Pattern.compile("^(#\\d+)$");

            protected StarboardCommandEmojisRemove(@Aware StarboardCommandEmojis owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Emoji(s).")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> {
                            String value = env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .orElseThrow();

                            List<EmojiData> emojis = starboardConfig.getEmojis();

                            if(indexModePattern.matcher(value).matches()){ // index mode
                                String str = value.substring(1);
                                if(!MessageUtil.canParseInt(str)){
                                    return messageService.err(env, "command.settings.emojis.overflow-index");
                                }

                                int idx = Strings.parseInt(str) - 1; // Counting the index from 1
                                if(idx < 0 || idx >= emojis.size()){
                                    return messageService.err(env, "command.settings.emojis.index-out-of-bounds");
                                }

                                EmojiData data = emojis.remove(idx);
                                return messageService.text(env, "command.settings.removed",
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
                                            return messageService.err(env, "command.settings.emojis.no-emojis");
                                        }

                                        emojis.removeAll(list);
                                        String str = list.stream()
                                                .map(DiscordUtil::getEmojiString)
                                                .collect(Collectors.joining(", "));
                                        return messageService.text(env, "command.settings.removed"
                                                        + (str.isBlank() ? "-nothing" : ""), str)
                                                .and(entityRetriever.save(starboardConfig));
                                    });
                        });
            }
        }

        @Subcommand(name = "clear", description = "Remove all emojis.")
        public static class StarboardCommandEmojisClear extends OwnerAwareCommand<StarboardCommandEmojis>{

            protected StarboardCommandEmojisClear(@Aware StarboardCommandEmojis owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

                return entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                        .flatMap(starboardConfig -> messageService.text(env, starboardConfig.getEmojis().isEmpty()
                                        ? "command.settings.removed-nothing"
                                        : "command.settings.emojis.clear")
                                .doFirst(starboardConfig.getEmojis()::clear)
                                .and(entityRetriever.save(starboardConfig)));
            }
        }
    }

    @Subcommand(name = "channel", description = "Configure starboard channel.")
    public static class StarboardCommandChannel extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandChannel(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New starboard channel.")
                    .type(ApplicationCommandOption.Type.CHANNEL.getValue()));
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env, "command.settings.starboard-channel.current",
                                            starboardConfig.getStarboardChannelId().map(DiscordUtil::getChannelMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(channelId -> {
                                starboardConfig.setStarboardChannelId(channelId);
                                return messageService.text(env, "command.settings.starboard-channel.update",
                                                DiscordUtil.getChannelMention(channelId))
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }

    @Subcommand(name = "self-starring", description = "Enable self starring.")
    public static class StarboardCommandSelfStarring extends OwnerAwareCommand<StarboardCommand>{

        protected StarboardCommandSelfStarring(@Aware StarboardCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            BooleanFunction<String> formatBool = bool ->
                    messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

            return entityRetriever.getStarboardConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createStarboardConfig(guildId))
                    .flatMap(starboardConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "command.settings.starboard-self-starring.update",
                                    formatBool.apply(starboardConfig.isSelfStarring())).then(Mono.never()))
                            .flatMap(bool -> {
                                starboardConfig.setSelfStarring(bool);
                                return messageService.text(env, "command.settings.starboard-self-starring.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(starboardConfig));
                            }));
        }
    }
}
