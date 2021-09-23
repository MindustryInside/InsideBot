package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.data.entity.AdminActionType;
import inside.interaction.*;
import inside.util.*;
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;

@InteractionDiscordCommand(name = "admin", description = "Admin settings.")
public class AdminCommand extends OwnerCommand{

    protected AdminCommand(@Aware List<? extends InteractionOwnerAwareCommand<AdminCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "warnings", description = "Configure max warnings count.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AdminCommandWarnings extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandWarnings(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New limit.")
                    .type(ApplicationCommandOptionType.INTEGER.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.warnings.current",
                                    adminConfig.getMaxWarnCount()).then(Mono.never()))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.negative-number").then(Mono.never()))
                            .flatMap(l -> {
                                adminConfig.setMaxWarnCount(l);
                                return messageService.text(env.event(), "command.settings.warnings.update", l)
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "duration", description = "Configure default duration.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AdminCommandDuration extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandDuration(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New base mute duration.")
                    .type(ApplicationCommandOptionType.STRING.getValue()));
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
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.base-duration.current",
                                    formatDuration.apply(adminConfig.getMuteBaseDelay())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration");
                                }

                                adminConfig.setMuteBaseDelay(duration);
                                return messageService.text(env.event(), "command.settings.base-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "mute-role", description = "Configure mute role.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AdminCommandMuteRole extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandMuteRole(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New mute role.")
                    .type(ApplicationCommandOptionType.ROLE.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.mute-role.current",
                                            adminConfig.getMuteRoleID().map(DiscordUtil::getRoleMention)
                                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(roleId -> {
                                adminConfig.setMuteRoleId(roleId);
                                return messageService.text(env.event(), "command.settings.mute-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "warn-duration", description = "Configure warn expire duration.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AdminCommandWarnDuration extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandWarnDuration(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New warn expire duration.")
                    .type(ApplicationCommandOptionType.STRING.getValue()));
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
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.warn-duration.current",
                                    formatDuration.apply(adminConfig.getMuteBaseDelay())).then(Mono.never()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration");
                                }

                                adminConfig.setWarnExpireDelay(duration);
                                return messageService.text(env.event(), "command.settings.warn-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "threshold-action", description = "Configure warn threshold action.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AdminCommandThresholdAction extends OwnerAwareCommand<AdminCommand>{

        protected AdminCommandThresholdAction(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("Action type.")
                    .type(ApplicationCommandOptionType.STRING.getValue())
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
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.threshold-action.current",
                                    String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                            adminConfig.getThresholdAction())).then(Mono.never()))
                            .flatMap(str -> {
                                AdminActionType action = Try.ofCallable(() -> AdminActionType.valueOf(str))
                                        .toOptional().orElseThrow(IllegalStateException::new);

                                adminConfig.setThresholdAction(action);
                                return messageService.text(env.event(), "command.settings.threshold-action.update",
                                                String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                                        adminConfig.getThresholdAction()))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "admin-roles", description = "Configure admin roles list.",
            type = ApplicationCommandOptionType.SUB_COMMAND_GROUP)
    public static class AdminCommandAdminRoles extends SubGroupOwnerCommand<AdminCommand>{

        protected AdminCommandAdminRoles(@Aware AdminCommand owner, @Aware List<? extends InteractionOwnerAwareCommand<AdminCommandAdminRoles>> subcommands){
            super(owner, subcommands);
        }

        @InteractionDiscordCommand(name = "list", description = "Display current admin roles list.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AdminCommandAdminRolesList extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesList(@Aware AdminCommandAdminRoles owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> messageService.text(env.event(), "command.settings.admin-roles.current",
                                Optional.of(adminConfig.getAdminRoleIds().stream()
                                                .map(DiscordUtil::getRoleMention)
                                                .collect(Collectors.joining(", ")))
                                        .filter(s -> !s.isBlank())
                                        .orElseGet(() -> messageService.get(env.context(), "command.settings.absents"))));
            }
        }

        @InteractionDiscordCommand(name = "add", description = "Add admin role(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AdminCommandAdminRolesAdd extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesAdd(@Aware AdminCommandAdminRoles owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("New admin role.")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("admin-roles")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = adminConfig.getAdminRoleIds();
                                    String[] text = value.split("(\\s+)?,(\\s+)?");

                                    return Flux.fromArray(text)
                                            .flatMap(str -> env.event().getInteraction().getGuild()
                                                    .flatMapMany(Guild::getRoles)
                                                    .filter(role -> MessageUtil.parseRoleId(str) != null &&
                                                            role.getId().equals(MessageUtil.parseRoleId(str)))
                                                    .doOnNext(role -> roleIds.add(role.getId())))
                                            .then(messageService.text(env.event(), "command.settings.added", roleIds.stream()
                                                    .map(DiscordUtil::getRoleMention)
                                                    .collect(Collectors.joining(", "))))
                                            .and(entityRetriever.save(adminConfig));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "remove", description = "Remove admin role(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AdminCommandAdminRolesRemove extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesRemove(@Aware AdminCommandAdminRoles owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Role.")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("admin-roles")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = adminConfig.getAdminRoleIds();
                                    List<Snowflake> removed = new ArrayList<>();
                                    String[] text = value.split("(\\s+)?,(\\s+)?");

                                    return Flux.fromArray(text)
                                            .flatMap(str -> env.event().getInteraction().getGuild()
                                                    .flatMapMany(Guild::getRoles)
                                                    .filter(role -> MessageUtil.parseRoleId(str) != null &&
                                                            role.getId().equals(MessageUtil.parseRoleId(str)))
                                                    .doOnNext(role -> {
                                                        if(roleIds.remove(role.getId())){
                                                            removed.add(role.getId());
                                                        }
                                                    }))
                                            .then(messageService.text(env.event(), "command.settings.removed", removed.stream()
                                                    .map(DiscordUtil::getRoleMention)
                                                    .collect(Collectors.joining(", "))))
                                            .and(entityRetriever.save(adminConfig));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "clear", description = "Remove all admin roles.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AdminCommandAdminRolesClear extends OwnerAwareCommand<AdminCommandAdminRoles>{

            protected AdminCommandAdminRolesClear(@Aware AdminCommandAdminRoles owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> {
                            adminConfig.getAdminRoleIds().clear();
                            return messageService.text(env.event(), "command.settings.admin-roles.clear")
                                    .and(entityRetriever.save(adminConfig));
                        });
            }
        }
    }
}
