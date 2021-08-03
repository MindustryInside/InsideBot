package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.Commands;
import inside.data.service.EntityRetriever;
import inside.interaction.InteractionCommandEnvironment;
import inside.service.*;
import inside.util.Mathf;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.format.*;

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

    @Autowired
    private AdminService adminService;

    @Override
    public Publisher<?> onButtonInteract(ButtonInteractEvent event){
        String id = event.getCustomId();
        if(!id.startsWith("inside")){
            return Mono.empty();
        }

        if(id.startsWith("inside-warnings")){
            String[] parts = id.split("-");
            Snowflake authorId = Snowflake.of(parts[2]);
            Snowflake targetId = Snowflake.of(parts[3]);
            int page = Integer.parseInt(parts[5]);

            Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
            Member target = event.getInteraction().getMember().orElse(null);
            if(guildId == null || target == null || !target.getId().equals(authorId)){
                return event.acknowledgeEphemeral();
            }

            int skipValues = page * Commands.WarningsCommand.PER_PAGE;

            Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                    .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                            KEY_TIMEZONE, guildConfig.timeZone()));

            return initContext.flatMap(context -> {

                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                        .withLocale(context.get(KEY_LOCALE))
                        .withZone(context.get(KEY_TIMEZONE));

                return adminService.warnings(guildId, targetId)
                        .index().skip(skipValues).take(Commands.WarningsCommand.PER_PAGE, true)
                        .map(TupleUtils.function((idx, warn) ->
                                EmbedCreateFields.Field.of(String.format("%2s. %s", idx + 1, formatter.format(warn.timestamp())), String.format("%s%n%s",
                                                messageService.format(context, "common.admin", warn.admin().effectiveName()),
                                                messageService.format(context, "common.reason", warn.reason()
                                                        .orElse(messageService.get(context, "common.not-defined")))),
                                        true)))
                        .collectList()
                        .zipWith(adminService.warnings(guildId, targetId).count())
                        .flatMap(TupleUtils.function((fields, count) -> event.edit(
                                InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .fields(fields)
                                                .color(settings.getDefaults().getNormalColor())
                                                .footer(String.format("Страница %s/%d", page + 1,
                                                        Mathf.ceilPositive(count / (float)Commands.WarningsCommand.PER_PAGE)), null)
                                                .build())
                                        .addComponent(ActionRow.of(
                                                Button.primary("inside-warnings-" + authorId.asString() +
                                                        "-" + targetId.asString() +
                                                        "-prev-" + (page - 1), messageService.get(context, "common.prev-page"))
                                                        .disabled(page - 1 < 0),
                                                Button.primary("inside-warnings-" + authorId.asString() +
                                                        "-" + targetId.asString() +
                                                        "-next-" + (page + 1), messageService.get(context, "common.next-page"))
                                                        .disabled(count <= skipValues + Commands.WarningsCommand.PER_PAGE)))
                                        .build())))
                        .contextWrite(context);
            });
        }

        return Mono.empty();
    }

    @Override
    public Publisher<?> onSlashCommand(SlashCommandEvent event){
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
