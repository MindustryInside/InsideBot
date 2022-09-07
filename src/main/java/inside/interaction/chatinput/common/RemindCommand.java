package inside.interaction.chatinput.common;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.User;
import inside.data.schedule.JobDetail;
import inside.data.schedule.ReactiveScheduler;
import inside.data.schedule.SimpleScheduleSpec;
import inside.data.schedule.Trigger;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Option;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import inside.service.job.RemindJob;
import inside.util.MessageUtil;
import io.r2dbc.postgresql.codec.Interval;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.UUID;

@ChatInputCommand("commands.common.remind")
@Option(name = "delay", type = Type.STRING, required = true)
@Option(name = "text", type = Type.STRING, required = true)
public class RemindCommand extends InteractionCommand {

    private final ReactiveScheduler reactiveScheduler;

    public RemindCommand(MessageService messageService, ReactiveScheduler reactiveScheduler) {
        super(messageService);
        this.reactiveScheduler = reactiveScheduler;
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {

        Interval delay = env.getOption("delay")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(MessageUtil::parseInterval)
                .orElse(null);

        if (delay == null) {
            return messageService.err(env, "common.invalid-interval-format");
        }

        String text = env.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow();

        Snowflake channelId = env.event().getInteraction().getChannelId();
        User author = env.event().getInteraction().getUser();

        JobDetail job = RemindJob.createDetails(author.getId(), channelId, text);

        Trigger trigger = SimpleScheduleSpec.builder()
                .startTimestamp(Instant.now().plus(delay))
                .key(RemindJob.GROUP, "trigger-" + UUID.randomUUID())
                .build()
                .asTrigger();

        return reactiveScheduler.scheduleJob(job, trigger)
                .then(messageService.text(env, "commands.common.remind.success"));
    }
}
