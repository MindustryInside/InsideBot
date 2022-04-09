package inside.service.job;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.http.client.ClientException;
import inside.Launcher;
import inside.data.EntityRetriever;
import inside.data.entity.ModerationAction;
import inside.data.schedule.Job;
import inside.data.schedule.JobDetail;
import inside.data.schedule.JobExecutionContext;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.UUID;

import static inside.service.job.SharedAttributeKeys.ID;

public class UnmuteJob implements Job {

    private final EntityRetriever entityRetriever;

    public static final String GROUP = "unmute";

    protected UnmuteJob(EntityRetriever entityRetriever) {
        this.entityRetriever = entityRetriever;
    }

    public static JobDetail createDetails(ModerationAction action) {
        return JobDetail.builder()
                .jobClass(UnmuteJob.class)
                .key(GROUP, "job-" + UUID.randomUUID())
                .putJobData("id", action.id())
                .build();
    }

    @Override
    public Publisher<?> execute(JobExecutionContext context) {
        long id = context.get(ID).orElseThrow();

        return entityRetriever.getModerationActionById(id)
                .zipWhen(a -> entityRetriever.getModerationConfigById(Snowflake.of(a.guildId())))
                .filter(TupleUtils.predicate((action, config) -> config.muteRoleId().isPresent()))
                .flatMap(TupleUtils.function((action, config) -> Launcher.getClient()
                        .getChannelById(Snowflake.of(action.targetId()))
                        .onErrorResume(e -> e instanceof ClientException c &&
                                c.getStatus().code() == 403, e -> Mono.empty()) // missing access
                        .cast(PrivateChannel.class)
                        .flatMap(c -> c.createMessage("Ваш мьют от админа %s%s подошёл к концу".formatted(
                                MessageUtil.getUserMention(action.adminId()), action.reason()
                                        .map(r -> " по причине *" + r + "*")
                                        .orElse(""))))
                        .and(Launcher.getClient().rest().getGuildService()
                                .removeGuildMemberRole(action.guildId(), action.targetId(),
                                        config.muteRoleId().orElseThrow(), "Прошёл срок мьюта"))
                        .and(entityRetriever.delete(action))));
    }
}
