package inside.interaction.component.button;

import discord4j.common.util.*;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.admin.WarningsCommand;
import inside.interaction.ButtonEnvironment;
import inside.interaction.annotation.ComponentProvider;
import inside.service.*;
import inside.util.Mathf;
import org.reactivestreams.Publisher;
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
    public Publisher<?> handle(ButtonEnvironment env){
        String[] parts = env.event().getCustomId().split("-");
        Snowflake authorId = Snowflake.of(parts[2]);
        Snowflake targetId = Snowflake.of(parts[3]);
        int page = Integer.parseInt(parts[5]);

        Member target = env.event().getInteraction().getMember().orElse(null);
        if(target == null || !target.getId().equals(authorId)){
            return messageService.err(env, messageService.get(env.context(), "message.foreign-interaction"));
        }

        int skipValues = page * WarningsCommand.PER_PAGE;

        return adminService.warnings(target.getGuildId(), targetId)
                .index().skip(skipValues).take(WarningsCommand.PER_PAGE, true)
                .map(function((idx, warn) ->
                        EmbedCreateFields.Field.of(String.format("%2s. %s", idx + 1,
                                        TimestampFormat.LONG_DATE_TIME.format(warn.getTimestamp())), String.format("%s%n%s",
                                        messageService.format(env.context(), "common.admin", warn.getAdmin().getEffectiveName()),
                                        messageService.format(env.context(), "common.reason", warn.getReason()
                                                .orElse(messageService.get(env.context(), "common.not-defined")))),
                                true)))
                .collectList()
                .zipWith(adminService.warnings(target.getGuildId(), targetId).count())
                .flatMap(function((fields, count) -> env.event().edit(
                        InteractionApplicationCommandCallbackSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .fields(fields)
                                        .title(messageService.format(env.context(), "command.admin.warnings.title",
                                                target.getDisplayName()))
                                        .color(settings.getDefaults().getNormalColor())
                                        .footer(messageService.format(env.context(), "command.admin.warnings.page", page + 1,
                                                Mathf.ceilPositive(count / (float)WarningsCommand.PER_PAGE)), null)
                                        .build())
                                .addComponent(ActionRow.of(
                                        Button.primary("inside-warnings-" + authorId.asString() +
                                                        "-" + targetId.asString() +
                                                        "-prev-" + (page - 1), messageService.get(env.context(), "common.prev-page"))
                                                .disabled(page - 1 < 0),
                                        Button.primary("inside-warnings-" + authorId.asString() +
                                                        "-" + targetId.asString() +
                                                        "-next-" + (page + 1), messageService.get(env.context(), "common.next-page"))
                                                .disabled(count <= skipValues + WarningsCommand.PER_PAGE)))
                                .build())));
    }
}
