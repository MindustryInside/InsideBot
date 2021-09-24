package inside.interaction.component;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.*;
import inside.command.model.*;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.*;

@ComponentProvider("inside-help")
public class HelpButtonListener implements ButtonListener{

    private final MessageService messageService;
    private final Settings settings;
    private final CommandHolder commandHolder;

    public HelpButtonListener(@Autowired MessageService messageService,
                              @Autowired Settings settings,
                              @Autowired CommandHolder commandHolder){
        this.messageService = messageService;
        this.settings = settings;
        this.commandHolder = commandHolder;
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event){
        return Mono.deferContextual(ctx -> {
            String[] parts = event.getCustomId().split("-"); // [ inside, help, 0, 0 ]
            Snowflake authorId = Snowflake.of(parts[3]);

            Member target = event.getInteraction().getMember().orElse(null);
            if(target == null || !target.getId().equals(authorId)){
                return messageService.err(event, messageService.get(ctx, "message.foreign-interaction"));
            }

            if(parts[2].equals("back")){
                var env = CommandEnvironment.builder()
                        .context(ctx)
                        .member(target)
                        .message(event.getMessage().orElseThrow())
                        .build();

                var categoryMap = commandHolder.getCommandInfoMap()
                        .entrySet().stream()
                        .collect(Collectors.groupingBy(e -> e.getValue().category()));

                return Flux.fromIterable(categoryMap.entrySet())
                        .filterWhen(entry -> Flux.fromIterable(entry.getValue())
                                .filterWhen(e -> e.getKey().filter(env))
                                .hasElements())
                        .map(e -> String.format("• %s (`%s`)%n",
                                messageService.getEnum(ctx, e.getKey()), e.getKey()))
                        .collect(Collectors.joining())
                        .map(s -> s.concat("\n").concat(messageService.get(ctx, "command.help.disclaimer.get-list")))
                        .flatMap(categoriesStr -> event.edit(InteractionApplicationCommandCallbackSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .title(messageService.get(ctx, "command.help"))
                                        .description(categoriesStr)
                                        .color(settings.getDefaults().getNormalColor())
                                        .build())
                                .addComponent(ActionRow.of(
                                        Arrays.stream(CommandCategory.all)
                                                .map(c -> Button.primary("inside-help-"
                                                                + c.ordinal() + "-" + target.getId().asString(),
                                                        messageService.getEnum(env.context(), c)))
                                                .toList()))
                                .build()));
            }

            int ordinal = Integer.parseInt(parts[2]);
            CommandCategory category = CommandCategory.all[ordinal];

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
                            builder.append(messageService.get(ctx, info.paramText()));
                        }
                        builder.append(" - ");
                        builder.append(messageService.get(ctx, info.description()));
                        builder.append("\n");
                    },
                    StringBuilder::append);

            return event.edit(InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .title(messageService.getEnum(ctx, category))
                            .description(commandHolder.getCommandInfoMap().values().stream()
                                    .sorted((o1, o2) -> Arrays.compare(o1.key(), o2.key()))
                                    .collect(categoryCollector)
                                    .append(messageService.get(ctx, "command.help.disclaimer.user")).append("\n")
                                    .append(messageService.get(ctx, "command.help.disclaimer.help"))
                                    .toString())
                            .color(settings.getDefaults().getNormalColor())
                            .build())
                    .addComponent(ActionRow.of(
                            Button.primary("inside-help-back-" + authorId.asString(),
                                    messageService.get(ctx, "command.help.button.return-back"))))
                    .build());
        });
    }
}
