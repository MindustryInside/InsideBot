package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import inside.annotation.Aware;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;

import static reactor.function.TupleUtils.function;

@ChatInputCommand(name = "prefixes", description = "Configure bot prefixes.")
public class PrefixesCommand extends OwnerCommand{

    protected PrefixesCommand(@Aware List<? extends InteractionOwnerAwareCommand<PrefixesCommand>> subcommands){
        super(subcommands);
    }

    @Subcommand(name = "list", description = "Display current prefixes.")
    public static class ListSubcommand extends OwnerAwareCommand<PrefixesCommand>{

        protected ListSubcommand(@Aware PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .flatMap(guildConfig -> messageService.text(env, "command.settings.prefix.current",
                            Optional.of(String.join(", ", guildConfig.prefixes()))
                                    .filter(s -> !s.isBlank())
                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absents"))));
        }
    }

    @Subcommand(name = "add", description = "Add prefix(s)")
    public static class AddSubcommand extends OwnerAwareCommand<PrefixesCommand>{

        protected AddSubcommand(@Aware PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New prefix(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .zipWith(Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)))
                    .flatMap(function((guildConfig, value) -> {
                        List<String> flags = guildConfig.prefixes();

                        String[] text = value.split("(\\s+)?,(\\s+)?");
                        Collections.addAll(flags, text);

                        return messageService.text(env, "command.settings.added"
                                        + (text.length == 0 ? "-nothing" : ""),
                                        String.join(", ", text))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @Subcommand(name = "remove", description = "Remove prefix(s).")
    public static class RemoveSubcommand extends OwnerAwareCommand<PrefixesCommand>{

        protected RemoveSubcommand(@Aware PrefixesCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Prefix(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

                        return messageService.text(env, "command.settings.removed"
                                        + (tmp.isEmpty() ? "-nothing" : ""),
                                        String.join(", ", tmp))
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @Subcommand(name = "clear", description = "Remove all prefixes")
    public static class ClearSubcommand extends OwnerAwareCommand<PrefixesCommand>{

        protected ClearSubcommand(@Aware PrefixesCommand owner){
            super(owner);
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .doOnNext(guildConfig -> guildConfig.prefixes().clear())
                    .flatMap(guildConfig -> messageService.text(env,
                            guildConfig.prefixes().isEmpty() ? "" :"command.settings.prefix.clear")
                            .and(entityRetriever.save(guildConfig)));
        }
    }
}
