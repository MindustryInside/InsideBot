package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
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

    @InteractionDiscordCommand(name = "list", description = "Display current prefixes.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class PrefixesCommandList extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandList(@Aware PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .flatMap(guildConfig -> messageService.text(env.event(), "command.settings.prefix.current",
                            Optional.of(String.join(", ", guildConfig.prefixes()))
                                    .filter(s -> !s.isBlank())
                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absents"))));
        }
    }

    @InteractionDiscordCommand(name = "add", description = "Add prefix(s)",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class PrefixesCommandAdd extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandAdd(@Aware PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New prefix(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
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

                        return messageService.text(env.event(), "command.settings.added"
                                        + (text.length == 0 ? "-nothing" : ""),
                                        String.join(", ", text))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "remove", description = "Remove prefix(s).",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class PrefixesCommandRemove extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandRemove(@Aware PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Prefix(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
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

                        return messageService.text(env.event(), "command.settings.removed"
                                        + (tmp.isEmpty() ? "-nothing" : ""),
                                        String.join(", ", tmp))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "clear", description = "Remove all prefixes",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class PrefixesCommandClear extends OwnerAwareCommand<PrefixesCommand>{

        protected PrefixesCommandClear(@Aware PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .doOnNext(guildConfig -> guildConfig.prefixes().clear())
                    .flatMap(guildConfig -> messageService.text(env.event(),
                            guildConfig.prefixes().isEmpty() ? "" :"command.settings.prefix.clear")
                            .and(entityRetriever.save(guildConfig)));
        }
    }
}
