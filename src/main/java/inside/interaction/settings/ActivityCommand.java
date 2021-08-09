package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static inside.util.ContextUtil.*;

@InteractionDiscordCommand(name = "activity", description = "Activity features settings.")
public class ActivityCommand extends OwnerCommand{

    protected ActivityCommand(@Aware List<? extends InteractionOwnerAwareCommand<?>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "", description = "",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ActivityCommandEnable extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandEnable(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New state.")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue()));
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
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.activities-enable.update",
                                    formatBool.apply(activityConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                activityConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.activities-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "active-user-role", description = "Configure active user role.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ActivityCommandActiveUserRole extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandActiveUserRole(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Active user role.")
                    .type(ApplicationCommandOptionType.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.active-user-role.current",
                                            activityConfig.getRoleId().map(DiscordUtil::getRoleMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(roleId -> {
                                activityConfig.setRoleId(roleId);
                                return messageService.text(env.event(), "command.settings.active-user-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "message-barrier", description = "Configure message barrier.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ActivityCommandMessageBarrier extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandMessageBarrier(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Minimal message count for reward activity.")
                    .type(ApplicationCommandOptionType.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getActivityConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId))
                    .flatMap(activityConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.message-barrier.current",
                                    activityConfig.getMessageBarrier()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.negative-number")
                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true)).then(Mono.never()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env.event(), "command.settings.overflow-number")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                activityConfig.setMessageBarrier(i);
                                return messageService.text(env.event(), "command.settings.message-barrier.update", i)
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "keep-counting-duration", description = "Configure keep counting duration.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class ActivityCommandKeepCountingDuration extends OwnerAwareCommand<ActivityCommand>{

        protected ActivityCommandKeepCountingDuration(@Aware ActivityCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Period value.")
                    .type(ApplicationCommandOptionType.STRING.getValue()));
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
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.keep-counting-duration.current",
                                    formatDuration.apply(activityConfig.getKeepCountingDuration())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                activityConfig.setKeepCountingDuration(duration);
                                return messageService.text(env.event(), "command.settings.keep-counting-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(activityConfig));
                            }));
        }
    }
}
