package inside.command.common;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.*;
import discord4j.rest.util.*;
import inside.command.Command;
import inside.command.model.*;
import reactor.core.publisher.Mono;

@DiscordCommand(key = "link", params = "command.link.params", description = "command.link.description")
public class LinkCommand extends Command{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Snowflake channelId = env.getMessage().getChannelId();
        String link = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        String text = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        return env.getClient().rest().getApplicationId()
                .flatMap(applicationId -> env.getClient().rest().getWebhookService()
                        .getChannelWebhooks(channelId.asLong())
                        .filter(data -> data.applicationId().map(id -> id.asLong() == applicationId).orElse(false)
                                && data.name().map(s -> env.getAuthorAsMember().getDisplayName().equals(s)).orElse(false))
                        .next()
                        .switchIfEmpty(env.getClient().rest().getWebhookService()
                                .createWebhook(channelId.asLong(), WebhookCreateRequest.builder()
                                        .name(env.getAuthorAsMember().getDisplayName())
                                        .avatarOrNull(env.getAuthorAsMember().getAvatarUrl())
                                        .build(), null))
                        .flatMap(webhookData -> env.getClient().rest().getWebhookService()
                                .executeWebhook(webhookData.id().asLong(), webhookData.token().get(), false,
                                        MultipartRequest.ofRequest(WebhookExecuteRequest.builder()
                                                .avatarUrl(env.getAuthorAsMember().getAvatarUrl())
                                                .content("[" + text + "](" + link + ")")
                                                .allowedMentions(AllowedMentions.suppressAll().toData())
                                                .build()))))
                .then(env.getMessage().delete());
    }
}
