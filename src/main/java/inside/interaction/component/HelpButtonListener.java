package inside.interaction.component;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.*;
import inside.command.model.CommandInfo;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collector;

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
            String[] parts = event.getCustomId().split("-");
            int ordinal = Integer.parseInt(parts[2]); // [ inside, help, 0, 0 ]
            Snowflake authorId = Snowflake.of(parts[3]);

            Member target = event.getInteraction().getMember().orElse(null);
            CommandCategory category = CommandCategory.all[ordinal];
            if(target == null || !target.getId().equals(authorId)){
                return messageService.err(event, messageService.get(ctx, "message.foreign-interaction"));
            }

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
                                            .toString())
                                    .color(settings.getDefaults().getNormalColor())
                                    .build())
                            .components(List.of()) // TODO: add 'return back' button
                    .build());
        });
    }
}
