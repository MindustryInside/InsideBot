package inside.command.common;

import discord4j.common.util.Snowflake;
import inside.command.*;
import inside.command.model.*;
import inside.data.entity.CommandConfig;
import inside.data.entity.base.ConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;

@DiscordCommand(key = "man", params = "command.man.params", description = "command.man.description")
public class ManCommand extends Command{
    private static final int SPACING = 32;
    private static final String PADDING = " ".repeat(SPACING);
    private static final String INDENT = "\t";
    private static final String SEPARATOR = "\n\n";

    @Autowired
    private CommandHolder commandHolder;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String name = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        Snowflake guildId = env.member().getGuildId();

        return Mono.justOrEmpty(commandHolder.getCommandInfo(name))
                .switchIfEmpty(entityRetriever.getCommandConfigById(guildId, name)
                        .filter(ConfigEntity::isEnabled)
                        .flatMap(s -> Mono.justOrEmpty(commandHolder.getCommandInfo(s.getNames().get(0)))))
                .switchIfEmpty(messageService.err(env, "command.man.not-found").then(Mono.never()))
                .zipWhen(info -> Flux.fromArray(info.key())
                        .concatWith(entityRetriever.getCommandConfigById(guildId, name)
                                .filter(ConfigEntity::isEnabled)
                                .flatMapIterable(CommandConfig::getAliases))
                        .collectList())
                .flatMap(TupleUtils.function((info, names) -> {
                    StringBuilder builder = new StringBuilder("```\n");

                    String nameCategory = messageService.get(env.context(), "command.man.category.name");
                    String synopsisCategory = messageService.get(env.context(), "command.man.category.synopsis");
                    String descriptionCategory = messageService.get(env.context(), "command.man.category.description");

                    String commandName0 = info.key()[0].toUpperCase() + "(1)";
                    builder.append(commandName0);
                    builder.append(PADDING).append(messageService.getEnum(env.context(), info.category()));
                    builder.append(PADDING).append(commandName0).append(SEPARATOR);

                    builder.append(nameCategory).append("\n");
                    builder.append(INDENT).append(String.join(", ", names));
                    builder.append(" - ").append(messageService.get(env.context(), info.description())).append(SEPARATOR);

                    builder.append(synopsisCategory).append("\n");
                    builder.append(INDENT).append(String.join(", ", names)).append(" ");
                    builder.append(messageService.get(env.context(), info.paramText()).toUpperCase()).append(SEPARATOR);

                    builder.append(descriptionCategory).append("\n");
                    String description = messageService.get(env.context(),
                            info.description() + "-full", info.description());
                    builder.append(INDENT).append(description).append(SEPARATOR);

                    builder.append("```");
                    return messageService.text(env, builder.toString())
                            .then();
                }));
    }
}
