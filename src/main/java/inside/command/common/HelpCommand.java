package inside.command.common;

import discord4j.core.object.component.*;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.*;
import inside.command.model.*;
import inside.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.*;

@DiscordCommand(key = {"help", "?", "man"}, params = "command.help.params", description = "command.help.description")
public class HelpCommand extends Command{
    @Autowired
    private CommandHolder commandHolder;

    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Optional<String> category = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(String::toLowerCase);

        Collector<CommandInfo, StringBuilder, StringBuilder> categoryCollector = Collector.of(StringBuilder::new,
                (builder, info) -> {
                    builder.append("**");
                    builder.append(info.key()[0]);
                    builder.append("**");
                    if(info.key().length > 1){
                        StringJoiner joiner = new StringJoiner(", ");
                        for(int i = 1; i < info.key().length; i++){
                            joiner.add(info.key()[i]);
                        }
                        builder.append(" (").append(joiner).append(")");
                    }
                    if(info.params().length > 0){
                        builder.append(" ");
                        builder.append(messageService.get(env.context(), info.paramText()));
                    }
                    builder.append(" - ");
                    builder.append(messageService.get(env.context(), info.description()));
                    builder.append("\n");
                },
                StringBuilder::append);

        var categoryMap = commandHolder.getCommandInfoMap()
                .entrySet().stream()
                .collect(Collectors.groupingBy(e -> e.getValue().category()));

        Mono<Void> categories = Flux.fromIterable(categoryMap.entrySet())
                .filterWhen(entry -> Flux.fromIterable(entry.getValue())
                        .filterWhen(e -> e.getKey().filter(env))
                        .hasElements())
                .map(e -> String.format("• %s (`%s`)%n", messageService.getEnum(env.context(), e.getKey()), e.getKey()))
                .collect(Collectors.joining())
                .map(s -> s.concat("\n").concat(messageService.get(env.context(), "command.help.disclaimer.get-list")))
                .flatMap(categoriesStr -> env.getReplyChannel().flatMap(channel -> channel.createMessage(
                        EmbedCreateSpec.builder()
                                .title(messageService.get(env.context(), "command.help"))
                                .description(categoriesStr)
                                .color(settings.getDefaults().getNormalColor())
                                .build())
                        .withComponents(ActionRow.of(
                                Arrays.stream(CommandCategory.all)
                                        .map(c -> Button.primary("inside-help-"
                                                + c.ordinal() + "-" + env.getAuthorAsMember().getId().asLong(),
                                                messageService.getEnum(env.context(), c)))
                                        .toList()))))
                .then();

        Mono<Void> snowHelp = Mono.defer(() -> {
            String unwrapped = category.orElse("");
            return categoryMap.keySet().stream()
                    .min(Comparator.comparingInt(s -> Strings.levenshtein(s.name(), unwrapped)))
                    .map(s -> messageService.err(env, "command.help.found-closest", s))
                    .orElse(messageService.err(env, "command.help.unknown"));
        });

        return Mono.justOrEmpty(category)
                .mapNotNull(s -> Try.ofCallable(() -> CommandCategory.valueOf(s)).orElse(null))
                .switchIfEmpty(categories.then(Mono.never()))
                .mapNotNull(categoryMap::get)
                .switchIfEmpty(snowHelp.then(Mono.never()))
                .filterWhen(entry -> Flux.fromIterable(entry)
                        .filterWhen(e -> e.getKey().filter(env))
                        .hasElements())
                .switchIfEmpty(messageService.err(env, "command.help.unknown").then(Mono.never()))
                .flatMapMany(Flux::fromIterable)
                .map(Map.Entry::getValue)
                .sort((o1, o2) -> Arrays.compare(o1.key(), o2.key()))
                .collect(categoryCollector)
                .map(builder -> builder.append(messageService.get(env.context(), "command.help.disclaimer.user"))
                        .append("\n").append(messageService.get(env.context(), "command.help.disclaimer.help")))
                .flatMap(str -> messageService.info(env, spec -> spec.title(messageService.getEnum(env.context(),
                                category.map(CommandCategory::valueOf).orElseThrow(IllegalStateException::new)))
                        .description(str.toString())));
    }
}
