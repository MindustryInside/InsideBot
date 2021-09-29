package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.data.entity.AdminActionType;
import inside.interaction.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;

@InteractionDiscordCommand(name = "admin", description = "Admin settings.")
public class AdminCommand extends OwnerCommand{

    protected AdminCommand(@Aware List<? extends InteractionOwnerAwareCommand<AdminCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "warnings", description = "Configure max warnings count.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class AdminCommandWarnings extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandWarnings(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New limit.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env, "command.settings.warnings.current",
                                    adminConfig.getMaxWarnCount()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env, "command.settings.negative-number").then(Mono.never()))
                            .flatMap(l -> {
                                adminConfig.setMaxWarnCount(l);
                                return messageService.text(env, "command.settings.warnings.update", l)
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "duration", description = "Configure default duration.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class AdminCommandDuration extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandDuration(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New base mute duration.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            Function<Duration, String> formatDuration = duration ->
                    DurationFormat.wordBased(env.context().get(KEY_LOCALE)).format(duration);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "command.settings.base-duration.current",
                                    formatDuration.apply(adminConfig.getMuteBaseDelay())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env, "command.settings.incorrect-duration");
                                }

                                adminConfig.setMuteBaseDelay(duration);
                                return messageService.text(env, "command.settings.base-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "mute-role", description = "Configure mute role.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class AdminCommandMuteRole extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandMuteRole(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New mute role.")
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env, "command.settings.mute-role.current",
                                            adminConfig.getMuteRoleID().map(DiscordUtil::getRoleMention)
                                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(roleId -> {
                                adminConfig.setMuteRoleId(roleId);
                                return messageService.text(env, "command.settings.mute-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "warn-duration", description = "Configure warn expire duration.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class AdminCommandWarnDuration extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandWarnDuration(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New warn expire duration.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            Function<Duration, String> formatDuration = duration ->
                    DurationFormat.wordBased(env.context().get(KEY_LOCALE)).format(duration);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "command.settings.warn-duration.current",
                                    formatDuration.apply(adminConfig.getMuteBaseDelay())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env, "command.settings.incorrect-duration");
                                }

                                adminConfig.setWarnExpireDelay(duration);
                                return messageService.text(env, "command.settings.warn-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "threshold-action", description = "Configure warn threshold action.",
            type = ApplicationCommandOption.Type.SUB_COMMAND)
    public static class AdminCommandThresholdAction extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandThresholdAction(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Action type.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .addChoice(ApplicationCommandOptionChoiceData.builder()
                            .name("ban")
                            .value("ban")
                            .build())
                    .addChoice(ApplicationCommandOptionChoiceData.builder()
                            .name("kick")
                            .value("kick")
                            .build())
                    .addChoice(ApplicationCommandOptionChoiceData.builder()
                            .name("mute")
                            .value("mute")
                            .build()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env, "command.settings.threshold-action.current",
                                    String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                            adminConfig.getThresholdAction())).then(Mono.never()))
                            .flatMap(str -> {
                                AdminActionType action = Try.ofCallable(() -> AdminActionType.valueOf(str))
                                        .toOptional().orElseThrow(IllegalStateException::new);

                                adminConfig.setThresholdAction(action);
                                return messageService.text(env, "command.settings.threshold-action.update",
                                                String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                                        adminConfig.getThresholdAction()))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "admin-roles", description = "Configure admin roles list.",
            type = ApplicationCommandOption.Type.SUB_COMMAND_GROUP)
    public static class AdminCommandAdminRoles extends SubGroupOwnerCommand<AdminCommand>{

        protected AdminCommandAdminRoles(@Aware AdminCommand owner, @Aware List<? extends InteractionOwnerAwareCommand<AdminCommandAdminRoles>> subcommands){
            super(owner, subcommands);
        }

        @InteractionDiscordCommand(name = "list", description = "Display current admin roles list.",
                type = ApplicationCommandOption.Type.SUB_COMMAND)
        public static class AdminCommandAdminRolesList extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesList(@Aware AdminCommandAdminRoles owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> messageService.text(env, "command.settings.admin-roles.current",
                                Optional.of(adminConfig.getAdminRoleIds().stream()
                                                .map(DiscordUtil::getRoleMention)
                                                .collect(Collectors.joining(", ")))
                                        .filter(s -> !s.isBlank())
                                        .orElseGet(() -> messageService.get(env.context(), "command.settings.absents"))));
            }
        }

        @InteractionDiscordCommand(name = "add", description = "Add admin role(s).",
                type = ApplicationCommandOption.Type.SUB_COMMAND)
        public static class AdminCommandAdminRolesAdd extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesAdd(@Aware AdminCommandAdminRoles owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("New admin role.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = Collections.synchronizedSet(adminConfig.getAdminRoleIds());
                                    String[] text = value.split("(\\s+)?,(\\s+)?");

                                    return Flux.fromArray(text)
                                            .flatMap(str -> env.getClient()
                                                    .withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                                    .getGuildRoles(guildId)
                                                    .filter(role -> role.getId().equals(MessageUtil.parseRoleId(str)) ||
                                                            role.getName().equalsIgnoreCase(str))
                                                    .mapNotNull(role -> roleIds.add(role.getId()) ? role.getMention() : null))
                                            .collect(Collectors.joining(", "))
                                            .flatMap(s -> messageService.text(env, "command.settings.added"
                                                    + (s.isBlank() ? "-nothing" : ""), s))
                                            .then(Mono.defer(() -> {
                                                adminConfig.setAdminRoleIds(roleIds);
                                                return entityRetriever.save(adminConfig);
                                            }));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "remove", description = "Remove admin role(s).",
                type = ApplicationCommandOption.Type.SUB_COMMAND)
        public static class AdminCommandAdminRolesRemove extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesRemove(@Aware AdminCommandAdminRoles owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Role ID or name.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = adminConfig.getAdminRoleIds();
                                    String[] text = value.split("(\\s+)?,(\\s+)?");

                                    return Flux.fromArray(text)
                                            .flatMap(str -> env.getClient()
                                                    .withRetrievalStrategy(EntityRetrievalStrategy.REST)
                                                    .getGuildRoles(guildId)
                                                    .filter(role -> role.getId().equals(MessageUtil.parseRoleId(str)) ||
                                                            role.getName().equalsIgnoreCase(str))
                                                    .mapNotNull(role -> roleIds.remove(role.getId()) ? role.getMention() : null))
                                            .collect(Collectors.joining(", "))
                                            .flatMap(s -> messageService.text(env, "command.settings.removed"
                                                    + (s.isBlank() ? "-nothing" : ""), s))
                                            .then(Mono.defer(() -> {
                                                adminConfig.setAdminRoleIds(roleIds);
                                                return entityRetriever.save(adminConfig);
                                            }));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "clear", description = "Remove all admin roles.",
                type = ApplicationCommandOption.Type.SUB_COMMAND)
        public static class AdminCommandAdminRolesClear extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesClear(@Aware AdminCommandAdminRoles owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> messageService.text(env, adminConfig.getAdminRoleIds().isEmpty()
                                        ? "command.settings.removed-nothing"
                                        : "command.settings.admin-roles.clear")
                                .doFirst(() -> adminConfig.setAdminRoleIds(Collections.emptySet()))
                                .and(entityRetriever.save(adminConfig)));
            }
        }
    }
}
