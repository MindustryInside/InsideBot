package inside.interaction.component;

import discord4j.common.util.*;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.admin.WarningsCommand;
import inside.service.*;
import inside.util.Mathf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import static reactor.function.TupleUtils.function;

@ComponentProvider("inside-warnings")
public class WarningsButtonListener implements ButtonListener{

    private final MessageService messageService;
    private final AdminService adminService;
    private final Settings settings;

    public WarningsButtonListener(@Autowired MessageService messageService,
                                  @Lazy @Autowired AdminService adminService,
                                  @Autowired Settings settings){
        this.messageService = messageService;
        this.adminService = adminService;
        this.settings = settings;
    }

    @Override
    public Mono<Void> handle(ButtonInteractionEvent event){
        return Mono.deferContextual(ctx -> {
            String[] parts = event.getCustomId().split("-");
            Snowflake authorId = Snowflake.of(parts[2]);
            Snowflake targetId = Snowflake.of(parts[3]);
            int page = Integer.parseInt(parts[5]);

            Member target = event.getInteraction().getMember().orElse(null);
            if(target == null || !target.getId().equals(authorId)){
                return messageService.err(event, messageService.get(ctx, "message.foreign-interaction"));
            }

            int skipValues = page * WarningsCommand.PER_PAGE;

            return adminService.warnings(target.getGuildId(), targetId)
                    .index().skip(skipValues).take(WarningsCommand.PER_PAGE, true)
                    .map(function((idx, warn) ->
                            EmbedCreateFields.Field.of(String.format("%2s. %s", idx + 1,
                                            TimestampFormat.LONG_DATE_TIME.format(warn.getTimestamp())), String.format("%s%n%s",
                                            messageService.format(ctx, "common.admin", warn.getAdmin().getEffectiveName()),
                                            messageService.format(ctx, "common.reason", warn.getReason()
                                                    .orElse(messageService.get(ctx, "common.not-defined")))),
                                    true)))
                    .collectList()
                    .zipWith(adminService.warnings(target.getGuildId(), targetId).count())
                    .flatMap(function((fields, count) -> event.edit(
                            InteractionApplicationCommandCallbackSpec.builder()
                                    .addEmbed(EmbedCreateSpec.builder()
                                            .fields(fields)
                                            .title(messageService.format(ctx, "command.admin.warnings.title",
                                                    target.getDisplayName()))
                                            .color(settings.getDefaults().getNormalColor())
                                            .footer(messageService.format(ctx, "command.admin.warnings.page", page + 1,
                                                    Mathf.ceilPositive(count / (float)WarningsCommand.PER_PAGE)), null)
                                            .build())
                                    .addComponent(ActionRow.of(
                                            Button.primary("inside-warnings-" + authorId.asString() +
                                                            "-" + targetId.asString() +
                                                            "-prev-" + (page - 1), messageService.get(ctx, "common.prev-page"))
                                                    .disabled(page - 1 < 0),
                                            Button.primary("inside-warnings-" + authorId.asString() +
                                                            "-" + targetId.asString() +
                                                            "-next-" + (page + 1), messageService.get(ctx, "common.next-page"))
                                                    .disabled(count <= skipValues + WarningsCommand.PER_PAGE)))
                                    .build())));
        });
    }
}
