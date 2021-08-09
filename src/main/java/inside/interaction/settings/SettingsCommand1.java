package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.audit.AuditActionType;
import inside.data.entity.AdminActionType;
import inside.interaction.InteractionCommandEnvironment;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.*;
import reactor.util.function.*;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

@Deprecated(forRemoval = true)
public class SettingsCommand1 extends OwnerCommand{

    private SettingsCommand1(){
        super(null);
    }

    private static <T> String formatCollection(Collection<? extends T> collection, Function<T, String> formatter){
        return collection.stream()
                .map(formatter)
                .collect(Collectors.joining(", "));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        Snowflake guildId = env.event().getInteraction().getGuildId()
                .orElseThrow(IllegalStateException::new);

        BooleanFunction<String> formatBool = bool ->
                messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

        Function<Duration, String> formatDuration = duration ->
                DurationFormat.wordBased(env.context().get(KEY_LOCALE)).format(duration);

        Mono<Void> handleAdmin = Mono.justOrEmpty(env.event().getOption("admin"))
                .zipWith(entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId)))
                .flatMap(function((group, adminConfig) -> {
                    Mono<Void> warnThresholdActionCommand = Mono.justOrEmpty(group.getOption("threshold-action")
                                    .flatMap(opt -> opt.getOption("value"))
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.threshold-action.current",
                                    String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                            adminConfig.getThresholdAction())).then(Mono.empty()))
                            .flatMap(str -> {
                                AdminActionType action = Try.ofCallable(() ->
                                        AdminActionType.valueOf(str)).toOptional().orElse(null);
                                Objects.requireNonNull(action, "action"); // impossible
                                adminConfig.setThresholdAction(action);

                                return messageService.text(env.event(), "command.settings.threshold-action.update",
                                                String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.getThresholdAction()),
                                                        adminConfig.getThresholdAction()))
                                        .and(entityRetriever.save(adminConfig));
                            });

                    Mono<Void> warningsCommand = Mono.justOrEmpty(group.getOption("warnings"))
                            .switchIfEmpty(warnThresholdActionCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.warnings.current",
                                    adminConfig.getMaxWarnCount()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .flatMap(number -> {
                                adminConfig.setMaxWarnCount(number);
                                return messageService.text(env.event(), "command.settings.warnings.update", number)
                                        .and(entityRetriever.save(adminConfig));
                            });

                    Mono<Void> muteRoleCommand = Mono.justOrEmpty(group.getOption("mute-role"))
                            .switchIfEmpty(warningsCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.mute-role.current",
                                            adminConfig.getMuteRoleID().map(DiscordUtil::getRoleMention)
                                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asRole))
                            .map(Role::getId)
                            .flatMap(roleId -> {
                                adminConfig.setMuteRoleId(roleId);
                                return messageService.text(env.event(), "command.settings.mute-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(adminConfig));
                            });

                    Mono<Void> adminRolesCommand = Mono.justOrEmpty(group.getOption("admin-roles"))
                            .switchIfEmpty(muteRoleCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                            .flatMap(ApplicationCommandInteractionOption::getValue))
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .filter(str -> !str.equals("help"))
                                    .switchIfEmpty(messageService.text(env.event(), "command.settings.admin-roles.current",
                                                    Optional.of(formatCollection(adminConfig.getAdminRoleIds(), DiscordUtil::getRoleMention))
                                                            .filter(s -> !s.isBlank())
                                                            .orElseGet(() -> messageService.get(env.context(), "command.settings.absents")))
                                            .then(Mono.empty()))
                                    .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                Set<Snowflake> roleIds = adminConfig.getAdminRoleIds();
                                if(choice.equals("clear")){
                                    roleIds.clear();
                                    return messageService.text(env.event(), "command.settings.admin-roles.clear");
                                }

                                boolean add = choice.equals("add");

                                List<Snowflake> removed = new ArrayList<>();
                                String[] text = enums.split("(\\s+)?,(\\s+)?");
                                Mono<Void> fetch = Flux.fromArray(text)
                                        .flatMap(str -> env.event().getInteraction().getGuild()
                                                .flatMapMany(Guild::getRoles)
                                                .filter(role -> MessageUtil.parseRoleId(str) != null &&
                                                        role.getId().equals(MessageUtil.parseRoleId(str)))
                                                .doOnNext(role -> {
                                                    if(add){
                                                        roleIds.add(role.getId());
                                                    }else{
                                                        if(roleIds.remove(role.getId())){
                                                            removed.add(role.getId());
                                                        }
                                                    }
                                                }))
                                        .then();

                                return fetch.then(Mono.defer(() -> {
                                    adminConfig.setAdminRoleIds(roleIds);
                                    if(add){
                                        return messageService.text(env.event(), "command.settings.added",
                                                formatCollection(roleIds, DiscordUtil::getRoleMention));
                                    }
                                    return messageService.text(env.event(), "command.settings.removed",
                                            formatCollection(removed, DiscordUtil::getRoleMention));
                                }));
                            }).and(entityRetriever.save(adminConfig))));

                    Mono<Void> warnDelayCommand = Mono.justOrEmpty(group.getOption("warn-duration"))
                            .switchIfEmpty(adminRolesCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.warn-duration.current",
                                    formatDuration.apply(adminConfig.getWarnExpireDelay())).then(Mono.empty()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                adminConfig.setWarnExpireDelay(duration);
                                return messageService.text(env.event(), "command.settings.warn-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            });

                    return Mono.justOrEmpty(group.getOption("duration"))
                            .switchIfEmpty(warnDelayCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.base-duration.current",
                                    formatDuration.apply(adminConfig.getMuteBaseDelay())).then(Mono.empty()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                adminConfig.setMuteBaseDelay(duration);
                                return messageService.text(env.event(), "command.settings.base-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            });
                }));

        return Mono.justOrEmpty(env.event().getOption("audit"))
                .switchIfEmpty(handleAdmin.then(Mono.empty()))
                .zipWith(entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId)))
                .flatMap(function((group, auditConfig) -> {
                    Mono<Void> channelCommand = Mono.justOrEmpty(group.getOption("channel")
                                    .flatMap(command -> command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.log-channel.current",
                                            auditConfig.getLogChannelId().map(DiscordUtil::getChannelMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asChannel))
                            .map(Channel::getId)
                            .flatMap(channelId -> {
                                auditConfig.setLogChannelId(channelId);
                                return messageService.text(env.event(), "command.settings.log-channel.update",
                                                DiscordUtil.getChannelMention(channelId))
                                        .and(entityRetriever.save(auditConfig));
                            });

                    Mono<Void> actionsCommand = Mono.justOrEmpty(group.getOption("actions"))
                            .switchIfEmpty(channelCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                            .flatMap(ApplicationCommandInteractionOption::getValue))
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .filter(str -> !str.equals("help"))
                                    .switchIfEmpty(Mono.defer(() -> {
                                        StringBuilder builder = new StringBuilder();
                                        var types = AuditActionType.all;
                                        for(int i = 0; i < types.length; i++){
                                            builder.append(messageService.getEnum(env.context(), types[i]));
                                            if(i + 1 != types.length){
                                                builder.append(", ");
                                            }
                                            if(i % 3 == 0){
                                                builder.append("\n");
                                            }
                                        }

                                        return messageService.text(env.event(), "command.settings.actions.all", builder);
                                    }).then(Mono.empty()))
                                    .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                            .switchIfEmpty(messageService.text(env.event(), "command.settings.actions.current",
                                                            formatCollection(auditConfig.getTypes(), type ->
                                                                    messageService.getEnum(env.context(), type)))
                                                    .then(Mono.empty()))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                Set<AuditActionType> flags = auditConfig.getTypes();
                                if(choice.equals("clear")){
                                    flags.clear();
                                    return messageService.text(env.event(), "command.settings.actions.clear");
                                }

                                List<Tuple2<AuditActionType, String>> all = Arrays.stream(AuditActionType.all)
                                        .map(type -> Tuples.of(type, messageService.getEnum(env.context(), type)))
                                        .toList();

                                boolean add = choice.equals("add");

                                Set<String> removed = new HashSet<>();
                                if(enums.equalsIgnoreCase("all") && add){
                                    flags.addAll(all.stream().map(Tuple2::getT1).collect(Collectors.toSet()));
                                }else{
                                    String[] text = enums.split("(\\s+)?,(\\s+)?");
                                    for(String s : text){
                                        all.stream().filter(predicate((type, str) -> str.equalsIgnoreCase(s)))
                                                .findFirst()
                                                .ifPresent(consumer((type, str) -> {
                                                    if(add){
                                                        flags.add(type);
                                                    }else{
                                                        if(flags.remove(type)){
                                                            removed.add(str);
                                                        }
                                                    }
                                                }));
                                    }
                                }

                                if(add){
                                    String formatted = formatCollection(flags, type ->
                                            messageService.getEnum(env.context(), type));

                                    return messageService.text(env.event(), "command.settings.added", formatted);
                                }
                                return messageService.text(env.event(), "command.settings.removed",
                                        String.join(", ", removed));
                            }).and(entityRetriever.save(auditConfig))));

                    return Mono.justOrEmpty(group.getOption("enable"))
                            .switchIfEmpty(actionsCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asBoolean)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.audit-enable.update",
                                    formatBool.apply(auditConfig.isEnabled())).then(Mono.empty()))
                            .flatMap(bool -> {
                                auditConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.audit-enable.update", formatBool.apply(bool))
                                        .and(entityRetriever.save(auditConfig));
                            });
                }));
    }

    @Override
    public ApplicationCommandRequest getRequest(){
        return ApplicationCommandRequest.builder()
                .name("settings")
                .description("Configure guild settings.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("audit")
                        .description("Audit log settings")
                        .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("enable")
                                .description("Enable audit logging")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Boolean value")
                                        .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("channel")
                                .description("Configure log channel")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Log channel")
                                        .type(ApplicationCommandOptionType.CHANNEL.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("actions")
                                .description("Configure bot locale")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("type")
                                        .description("Action type")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .required(true)
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Get a help")
                                                .value("help")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Add audit action type(s)")
                                                .value("add")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove audit action type(s)")
                                                .value("remove")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove all audit actions")
                                                .value("clear")
                                                .build())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Audit action type")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("admin")
                        .description("Admin settings")
                        .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("warnings")
                                .description("Configure max warnings count")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Limit count")
                                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("duration")
                                .description("Configure default duration")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Duration")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("mute-role")
                                .description("Configure mute role")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Mute role")
                                        .type(ApplicationCommandOptionType.ROLE.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("warn-duration")
                                .description("Configure warn expire duration")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Duration")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("threshold-action")
                                .description("Configure warn threshold action")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Action type")
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
                                                .build())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("admin-roles")
                                .description("Configure bot locale")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("type")
                                        .description("Action type")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .required(true)
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Get a help")
                                                .value("help")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Add admin role(s)")
                                                .value("add")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove admin role(s)")
                                                .value("remove")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove all admin roles")
                                                .value("clear")
                                                .build())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Admin role")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
