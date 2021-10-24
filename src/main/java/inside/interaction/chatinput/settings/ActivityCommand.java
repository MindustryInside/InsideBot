package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import inside.annotation.Aware;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import inside.util.func.BooleanFunction;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static inside.util.ContextUtil.KEY_LOCALE;

@ChatInputCommand(name = "activity", description = "Activity features settings.")
public class ActivityCommand extends OwnerCommand{

    protected ActivityCommand(@Aware List<? extends InteractionOwnerAwareCommand<ActivityCommand>> subcommands){
        super(subcommands);
    }

    @Subcommand(name = "enable", description = "Enable activity system.")
    public static class EnableSubcommand extends OwnerAwareCommand<ActivityCommand>{

        protected EnableSubcommand(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            BooleanFunction<String> formatBool = bool ->
                    messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env, "command.settings.activities-enable.update",
                                    formatBool.apply(activityConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                activityConfig.setEnabled(bool);
                                return messageService.text(env, "command.settings.activities-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @Subcommand(name = "active-user-role", description = "Configure active user role.")
    public static class ActiveUserRoleSubcommand extends OwnerAwareCommand<ActivityCommand>{

        protected ActiveUserRoleSubcommand(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New active user role.")
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env, "command.settings.active-user-role.current",
                                            activityConfig.getRoleId().map(DiscordUtil::getRoleMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(roleId -> {
                                activityConfig.setRoleId(roleId);
                                return messageService.text(env, "command.settings.active-user-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @Subcommand(name = "message-barrier", description = "Configure message barrier.")
    public static class MessageBarrierSubcommand extends OwnerAwareCommand<ActivityCommand>{

        protected MessageBarrierSubcommand(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New minimal message count for reward activity.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env, "command.settings.message-barrier.current",
                                    activityConfig.getMessageBarrier()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env, "command.settings.negative-number").then(Mono.never()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env, "command.settings.overflow-number");
                                }

                                activityConfig.setMessageBarrier(i);
                                return messageService.text(env, "command.settings.message-barrier.update", i)
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @Subcommand(name = "keep-counting-duration", description = "Configure keep counting duration.")
    public static class KeepCountingDurationSubcommand extends OwnerAwareCommand<ActivityCommand>{

        protected KeepCountingDurationSubcommand(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New period.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

            Function<Duration, String> formatDuration = duration ->
                    DurationFormat.wordBased(env.context().get(KEY_LOCALE)).format(duration);

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "command.settings.keep-counting-duration.current",
                                    formatDuration.apply(activityConfig.getKeepCountingDuration())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env, "command.settings.incorrect-duration");
                                }

                                activityConfig.setKeepCountingDuration(duration);
                                return messageService.text(env, "command.settings.keep-counting-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }
}
