package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import inside.interaction.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static inside.util.ContextUtil.KEY_LOCALE;

@InteractionDiscordCommand(name = "activity", description = "Activity features settings.")
public class ActivityCommand extends OwnerCommand{

    protected ActivityCommand(@Aware List<? extends InteractionOwnerAwareCommand<ActivityCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "enable", description = "Enable activity system.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class ActivityCommandEnable extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandEnable(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

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

    @InteractionDiscordCommand(name = "active-user-role", description = "Configure active user role.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class ActivityCommandActiveUserRole extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandActiveUserRole(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New active user role.")
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

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

    @InteractionDiscordCommand(name = "message-barrier", description = "Configure message barrier.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class ActivityCommandMessageBarrier extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandMessageBarrier(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New minimal message count for reward activity.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

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

    @InteractionDiscordCommand(name = "keep-counting-duration", description = "Configure keep counting duration.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class ActivityCommandKeepCountingDuration extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandKeepCountingDuration(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New period.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

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
