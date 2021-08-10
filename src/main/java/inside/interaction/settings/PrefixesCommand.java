package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.util.*;

import static reactor.function.TupleUtils.function;

@InteractionDiscordCommand(name = "prefixes", description = "Configure bot prefixes.")
public class PrefixesCommand extends OwnerCommand{

    protected PrefixesCommand(@Aware List<? extends InteractionOwnerAwareCommand<PrefixesCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "help", description = "Get a help.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class PrefixesCommandHelp extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandHelp(@Aware PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .flatMap(guildConfig -> messageService.text(env.event(), "command.settings.prefix.current",
                            String.join(", ", guildConfig.prefixes())));
        }
    }

    @InteractionDiscordCommand(name = "add", description = "Add prefix(s)",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class PrefixesCommandAdd extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandAdd(@Lazy PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Target prefix(s)")
                    .required(true)
                    .type(ApplicationCommandOptionType.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .zipWith(Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)))
                    .flatMap(function((guildConfig, value) -> {
                        List<String> flags = guildConfig.prefixes();

                        String[] text = value.split("(\\s+)?,(\\s+)?");
                        Collections.addAll(flags, text);

                        return messageService.text(env.event(), "command.settings.added",
                                String.join(", ", text))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "remove", description = "Remove prefix(s).",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class PrefixesCommandRemove extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandRemove(@Lazy PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Target prefix(s)")
                    .required(true)
                    .type(ApplicationCommandOptionType.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .zipWith(Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)))
                    .flatMap(function((guildConfig, value) -> {
                        List<String> flags = guildConfig.prefixes();
                        List<String> tmp = new ArrayList<>(flags);

                        String[] text = value.split("(\\s+)?,(\\s+)?");
                        flags.removeAll(Arrays.asList(text));
                        tmp.removeAll(flags);

                        return messageService.text(env.event(), "command.settings.added",
                                        String.join(", ", tmp))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "clear", description = "Remove all prefixes",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class PrefixesCommandClear extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandClear(@Lazy PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .doOnNext(guildConfig -> guildConfig.prefixes().clear())
                    .flatMap(guildConfig -> messageService.text(env.event(), "command.settings.prefix.clear")
                            .and(entityRetriever.save(guildConfig)));
        }
    }
}
