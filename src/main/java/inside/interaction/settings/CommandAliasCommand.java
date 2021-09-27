package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import inside.command.CommandHolder;
import inside.interaction.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.Mono;

import java.util.*;

import static reactor.function.TupleUtils.function;

@InteractionDiscordCommand(name = "command-alias", description = "Configure command aliases.")
public class CommandAliasCommand extends OwnerCommand{

    protected final CommandHolder commandHolder;

    protected CommandAliasCommand(@Aware List<? extends InteractionOwnerAwareCommand<CommandAliasCommand>> subcommands,
                                  @Aware CommandHolder commandHolder){
        super(subcommands);
        this.commandHolder = commandHolder;
    }

    @InteractionDiscordCommand(name = "enable", description = "Enable command configuring.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class CommandAliasCommandEnable extends OwnerAwareCommand<CommandAliasCommand>{

        protected CommandAliasCommandEnable(@Aware CommandAliasCommand owner){
            super(owner);

            addOption(builder -> builder.name("name")
                    .description("Command name.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            BooleanFunction<String> formatBool = bool ->
                    messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

            return Mono.justOrEmpty(env.getOption("name")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString))
                    .map(String::toLowerCase)
                    .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                            .switchIfEmpty(Mono.justOrEmpty(owner.commandHolder.getCommandInfo(s))
                                    .flatMap(info -> entityRetriever.createCommandConfig(
                                            guildId, Arrays.asList(info.key()), List.of())))
                            .switchIfEmpty(messageService.err(env.event(), "command.settings.command-alias.not-found").then(Mono.never())))
                    .flatMap(commandConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.command-alias.current",
                                    formatBool.apply(commandConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                commandConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.command-alias.update-enable",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(commandConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "list", description = "Display current command alias list.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class CommandAliasCommandList extends OwnerAwareCommand<CommandAliasCommand>{

        protected CommandAliasCommandList(@Aware CommandAliasCommand owner){
            super(owner);

            addOption(builder -> builder.name("name")
                    .description("Command name.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId()
                    .orElseThrow(IllegalStateException::new);

            String name = env.getOption("name")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(IllegalStateException::new);

            return Mono.justOrEmpty(name)
                    .map(String::toLowerCase)
                    .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                            .switchIfEmpty(Mono.justOrEmpty(owner.commandHolder.getCommandInfo(s))
                                    .flatMap(info -> entityRetriever.createCommandConfig(
                                            guildId, Arrays.asList(info.key()), List.of())))
                            .switchIfEmpty(messageService.err(env.event(), "command.settings.command-alias.not-found").then(Mono.never())))
                    .flatMap(commandConfig -> messageService.text(env.event(),
                            commandConfig.getAliases().isEmpty()
                                    ? "command.settings.command-alias.absent"
                                    : "command.settings.command-alias.current",
                            name, String.join(", ", commandConfig.getAliases())));
        }
    }

    @InteractionDiscordCommand(name = "add", description = "Add alias(s)",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class CommandAliasCommandAdd extends OwnerAwareCommand<CommandAliasCommand>{

        protected CommandAliasCommandAdd(@Aware CommandAliasCommand owner){
            super(owner);

            addOption(builder -> builder.name("name")
                    .description("Command name.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("value")
                    .description("New alias(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return Mono.justOrEmpty(env.getOption("name")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString))
                    .map(String::toLowerCase)
                    .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                            .switchIfEmpty(Mono.justOrEmpty(owner.commandHolder.getCommandInfo(s))
                                    .flatMap(info -> entityRetriever.createCommandConfig(
                                            guildId, Arrays.asList(info.key()), List.of())))
                            .switchIfEmpty(messageService.err(env.event(), "command.settings.command-alias.not-found").then(Mono.never())))
                    .zipWith(Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)))
                    .flatMap(function((commandConfig, value) -> {
                        List<String> flags = commandConfig.getAliases();

                        String[] text = value.split("(\\s+)?,(\\s+)?");
                        Collections.addAll(flags, text);

                        return messageService.text(env.event(), "command.settings.added",
                                        String.join(", ", text))
                                .and(entityRetriever.save(commandConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "remove", description = "Remove alias(s).",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class CommandAliasCommandRemove extends OwnerAwareCommand<CommandAliasCommand>{

        protected CommandAliasCommandRemove(@Aware CommandAliasCommand owner){
            super(owner);

            addOption(builder -> builder.name("name")
                    .description("Command name.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));

            addOption(builder -> builder.name("value")
                    .description("Alias(s).")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return Mono.justOrEmpty(env.getOption("name")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString))
                    .map(String::toLowerCase)
                    .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                            .switchIfEmpty(Mono.justOrEmpty(owner.commandHolder.getCommandInfo(s))
                                    .flatMap(info -> entityRetriever.createCommandConfig(
                                            guildId, Arrays.asList(info.key()), List.of())))
                            .switchIfEmpty(messageService.err(env.event(), "command.settings.command-alias.not-found").then(Mono.never())))
                    .zipWith(Mono.justOrEmpty(env.getOption("value")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)))
                    .flatMap(function((commandConfig, value) -> {
                        List<String> flags = commandConfig.getAliases();
                        List<String> tmp = new ArrayList<>(flags);

                        String[] text = value.split("(\\s+)?,(\\s+)?");
                        flags.removeAll(Arrays.asList(text));
                        tmp.removeAll(flags);

                        return messageService.text(env.event(), "command.settings.removed",
                                        String.join(", ", tmp))
                                .and(entityRetriever.save(commandConfig));
                    }));
        }
    }

    @InteractionDiscordCommand(name = "clear", description = "Remove all aliases.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class CommandAliasCommandClear extends OwnerAwareCommand<CommandAliasCommand>{

        protected CommandAliasCommandClear(@Aware CommandAliasCommand owner){
            super(owner);

            addOption(builder -> builder.name("name")
                    .description("Command name.")
                    .required(true)
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return Mono.justOrEmpty(env.getOption("name")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString))
                    .map(String::toLowerCase)
                    .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                            .switchIfEmpty(Mono.justOrEmpty(owner.commandHolder.getCommandInfo(s))
                                    .flatMap(info -> entityRetriever.createCommandConfig(
                                            guildId, Arrays.asList(info.key()), List.of())))
                            .switchIfEmpty(messageService.err(env.event(), "command.settings.command-alias.not-found").then(Mono.never())))
                    .doOnNext(configAliases -> configAliases.getAliases().clear())
                    .flatMap(commandConfig -> messageService.text(env.event(), "command.settings.command-alias.clear")
                            .and(entityRetriever.save(commandConfig)));
        }
    }
}
