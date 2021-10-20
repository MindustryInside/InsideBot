package inside.command.common;

import discord4j.core.object.entity.Member;
import inside.command.Command;
import inside.command.model.*;
import inside.scheduler.job.RemindJob;
import inside.util.Try;
import org.quartz.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Date;

import static inside.service.MessageService.ok;

@DiscordCommand(key = "remind", params = "command.remind.params", description = "command.remind.description")
public class RemindCommand extends Command{
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        ZonedDateTime time = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asDateTime)
                .orElse(null);

        String text = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        if(time == null){
            return messageService.err(env, "message.error.invalid-time");
        }

        Member member = env.member();
        JobDetail job = RemindJob.createDetails(member.getGuildId(), member.getId(),
                env.message().getChannelId(), text);

        Trigger trigger = TriggerBuilder.newTrigger()
                .startAt(Date.from(time.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .build();

        Try.run(() -> schedulerFactoryBean.getScheduler().scheduleJob(job, trigger));
        return env.message().addReaction(ok);
    }
}
