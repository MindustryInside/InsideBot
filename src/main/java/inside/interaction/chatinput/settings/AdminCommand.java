package inside.interaction.chatinput.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.annotation.Aware;
import inside.data.entity.AdminActionType;
import inside.interaction.*;
import inside.interaction.annotation.*;
import inside.interaction.chatinput.*;
import inside.util.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.*;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;

@ChatInputCommand(name = "admin", description = "Admin settings.")
public class AdminCommand extends OwnerCommand{

    protected AdminCommand(@Aware List<? extends InteractionOwnerAwareCommand<AdminCommand>> subcommands){
        super(subcommands);
    }

    @Subcommand(name = "warnings", description = "Configure max warnings count.")
    public static class WarningsSubcommand extends OwnerAwareCommand<AdminCommand>{

        protected WarningsSubcommand(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New limit.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

    @Subcommand(name = "duration", description = "Configure default duration.")
    public static class DurationSubcommand extends OwnerAwareCommand<AdminCommand>{

        protected DurationSubcommand(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New base mute duration.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

    @Subcommand(name = "mute-role", description = "Configure mute role.")
    public static class MuteRoleSubcommand extends OwnerAwareCommand<AdminCommand>{

        protected MuteRoleSubcommand(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New mute role.")
                    .type(ApplicationCommandOption.Type.ROLE.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

    @Subcommand(name = "warn-duration", description = "Configure warn expire duration.")
    public static class WarnDurationSubcommand extends OwnerAwareCommand<AdminCommand>{

        protected WarnDurationSubcommand(@Aware AdminCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New warn expire duration.")
                    .type(ApplicationCommandOption.Type.STRING.getValue()));
        }

        @Override
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

    @Subcommand(name = "threshold-action", description = "Configure warn threshold action.")
    public static class ThresholdActionSubcommand extends OwnerAwareCommand<AdminCommand>{

        protected ThresholdActionSubcommand(@Aware AdminCommand owner){
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
        public Publisher<?> execute(CommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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
                                        .toOptional().orElseThrow();

                                adminConfig.setThresholdAction(action);
                                return messageService.text(env, "command.settings.threshold-action.update",
                                                String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                                        adminConfig.getThresholdAction()))
                                        .and(entityRetriever.save(adminConfig));
                            }));
        }
    }

    @SubcommandGroup(name = "admin-roles", description = "Configure admin roles list.")
    public static class AdminRolesSubcommandGroup extends SubGroupOwnerCommand<AdminCommand>{

        protected AdminRolesSubcommandGroup(@Aware AdminCommand owner, @Aware List<? extends InteractionOwnerAwareCommand<AdminRolesSubcommandGroup>> subcommands){
            super(owner, subcommands);
        }

        @Subcommand(name = "list", description = "Display current admin roles list.")
        public static class AdminRolesListSubcommand extends OwnerAwareCommand<AdminRolesSubcommandGroup>{

            protected AdminRolesListSubcommand(@Aware AdminRolesSubcommandGroup owner){
                super(owner);
            }

            @Override
            public Publisher<?> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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

        @Subcommand(name = "add", description = "Add admin role(s).")
        public static class AdminRolesAddSubcommand extends OwnerAwareCommand<AdminRolesSubcommandGroup>{

            protected AdminRolesAddSubcommand(@Aware AdminRolesSubcommandGroup owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("New admin role.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = Collections.synchronizedSet(adminConfig.getAdminRoleIds());
                                    String[] text = value.split("\\s*,\\s*");

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

        @Subcommand(name = "remove", description = "Remove admin role(s).")
        public static class AdminRolesRemoveSubcommand extends OwnerAwareCommand<AdminRolesSubcommandGroup>{

            protected AdminRolesRemoveSubcommand(@Aware AdminRolesSubcommandGroup owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Role ID or name.")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue()));
            }

            @Override
            public Publisher<?> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

                return entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                        .flatMap(adminConfig -> Mono.justOrEmpty(env.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<Snowflake> roleIds = adminConfig.getAdminRoleIds();
                                    String[] text = value.split("\\s*,\\s*");

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

        @Subcommand(name = "clear", description = "Remove all admin roles.")
        public static class AdminRolesClearSubcommand extends OwnerAwareCommand<AdminRolesSubcommandGroup>{

            protected AdminRolesClearSubcommand(@Aware AdminRolesSubcommandGroup owner){
                super(owner);
            }

            @Override
            public Publisher<?> execute(CommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

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
