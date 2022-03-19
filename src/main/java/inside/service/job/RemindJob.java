package inside.service.job;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import inside.Launcher;
import inside.data.schedule.Job;
import inside.data.schedule.JobDetail;
import inside.data.schedule.JobExecutionContext;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static inside.service.job.SharedAttributeKeys.*;

public class RemindJob implements Job {
    public static final String GROUP = "remind";

    public static JobDetail createDetails(Snowflake userId, Snowflake channelId, String message) {
        return JobDetail.builder()
                .jobClass(RemindJob.class)
                .key(GROUP, "job-" + UUID.randomUUID())
                .putJobData("user_id", userId)
                .putJobData("channel_id", channelId)
                .putJobData("message", message)
                .build();
    }

    @Override
    public Publisher<?> execute(JobExecutionContext context) {
        Snowflake userId = context.get(USER_ID).orElseThrow();
        Snowflake channelId = context.get(CHANNEL_ID).orElseThrow();
        String text = context.get(MESSAGE).orElseThrow();

        return Launcher.getClient().getChannelById(channelId)
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .content(String.format("%s, %s", MessageUtil.getUserMention(userId), text))
                        .allowedMentions(AllowedMentions.builder()
                                .allowUser(userId)
                                .build())
                        .build()))
                .onErrorResume(e -> e instanceof ClientException c &&
                        c.getStatus().code() == 403, e -> Mono.empty()); // missing access
    }
}
