package inside.event;

import discord4j.common.util.*;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.admin.WarningsCommand;
import inside.data.service.EntityRetriever;
import inside.interaction.InteractionCommandEnvironment;
import inside.service.*;
import inside.util.Mathf;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import static inside.util.ContextUtil.*;

@Component
public class InteractionEventHandler extends ReactiveEventAdapter{

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Settings settings;

    @Autowired
    private DiscordService discordService;

    @Lazy
    @Autowired
    private AdminService adminService;

    @Override
    public Publisher<?> onButtonInteraction(ButtonInteractionEvent event){
        String id = event.getCustomId();
        if(!id.startsWith("inside")){
            return Mono.empty();
        }

        Snowflake guildId = event.getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        return initContext.flatMap(ctx -> Mono.defer(() -> {
            if(id.startsWith("inside-warnings")){
                String[] parts = id.split("-");
                Snowflake authorId = Snowflake.of(parts[2]);
                Snowflake targetId = Snowflake.of(parts[3]);
                int page = Integer.parseInt(parts[5]);

                Member target = event.getInteraction().getMember().orElse(null);
                if(target == null || !target.getId().equals(authorId)){
                    return messageService.err(event, messageService.get(ctx, "message.foreign-interaction"));
                }

                int skipValues = page * WarningsCommand.PER_PAGE;

                return adminService.warnings(guildId, targetId)
                        .index().skip(skipValues).take(WarningsCommand.PER_PAGE, true)
                        .map(TupleUtils.function((idx, warn) ->
                                EmbedCreateFields.Field.of(String.format("%2s. %s", idx + 1,
                                                TimestampFormat.LONG_DATE_TIME.format(warn.getTimestamp())), String.format("%s%n%s",
                                                messageService.format(ctx, "common.admin", warn.getAdmin().getEffectiveName()),
                                                messageService.format(ctx, "common.reason", warn.getReason()
                                                        .orElse(messageService.get(ctx, "common.not-defined")))),
                                        true)))
                        .collectList()
                        .zipWith(adminService.warnings(guildId, targetId).count())
                        .flatMap(TupleUtils.function((fields, count) -> event.edit(
                                InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .fields(fields)
                                                .title(messageService.get(ctx, "command.admin.warnings.title"))
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
            }

            return Mono.empty();
        })
        .contextWrite(ctx));
    }

    @Override
    public Publisher<?> onApplicationCommandInteraction(ApplicationCommandInteractionEvent event){
        Mono<Context> initContext = Mono.justOrEmpty(event.getInteraction().getGuildId())
                .flatMap(guildId -> entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createGuildConfig(guildId)))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()))
                .defaultIfEmpty(Context.of(KEY_LOCALE, messageService.getDefaultLocale(),
                        KEY_TIMEZONE, settings.getDefaults().getTimeZone()));

        return initContext.flatMap(context -> discordService.handle(InteractionCommandEnvironment.builder()
                        .event(event)
                        .context(context)
                        .build())
                .contextWrite(context));
    }
}
