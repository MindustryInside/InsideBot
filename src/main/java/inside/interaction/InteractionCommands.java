package inside.interaction;

import com.udojava.evalex.Expression;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.Settings;
import inside.audit.*;
import inside.command.Commands;
import inside.data.service.AdminService;
import inside.service.MessageService;
import inside.util.*;
import inside.util.io.ReusableByteInputStream;
import org.joda.time.*;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static inside.audit.Attribute.COUNT;
import static inside.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

public class InteractionCommands{

    private InteractionCommands(){}

    public static abstract class GuildCommand extends InteractionCommand{
        @Autowired
        protected Settings settings;

        @Override
        public Mono<Boolean> apply(InteractionCommandEnvironment env){
            if(env.event().getInteraction().getMember().isEmpty()){
                return messageService.err(env.event(), "command.interaction.only-guild").then(Mono.empty());
            }
            return Mono.just(true);
        }
    }

    public static abstract class AdminCommand extends GuildCommand{
        @Lazy
        @Autowired
        protected AdminService adminService;

        @Override
        public Mono<Boolean> apply(InteractionCommandEnvironment env){
            Mono<Boolean> isAdmin = env.event().getInteraction().getMember()
                    .map(adminService::isAdmin)
                    .orElse(Mono.just(false));

            return BooleanUtils.and(super.apply(env), isAdmin);
        }
    }

    @InteractionDiscordCommand
    public static class SettingsCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId()
                    .orElseThrow(AssertionError::new);

            Mono<Void> handleAdmin = Mono.justOrEmpty(env.event().getInteraction().getCommandInteraction()
                    .getOption("admin"))
                    .zipWith(entityRetriever.getAdminConfigById(guildId)
                            .switchIfEmpty(entityRetriever.createAdminConfig(guildId)))
                    .flatMap(function((group, adminConfig) -> {
                        Mono<Void> warningsCommand = Mono.justOrEmpty(group.getOption("warnings")
                                .flatMap(command -> command.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.warnings.current",
                                        adminConfig.maxWarnCount()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asLong))
                                .flatMap(number -> Mono.defer(() -> {
                                    adminConfig.maxWarnCount(number);
                                    return messageService.text(env.event(), "command.settings.warnings.update", number)
                                            .and(entityRetriever.save(adminConfig));
                                }));

                        Mono<Void> muteRoleCommand = Mono.justOrEmpty(group.getOption("mute-role"))
                                .switchIfEmpty(warningsCommand.then(Mono.empty()))
                                .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.mute-role.current",
                                        adminConfig.muteRoleID().map(DiscordUtil::getRoleMention)
                                                .orElse(messageService.get(env.context(), "command.settings.absent")))
                                        .then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .flatMap(ApplicationCommandInteractionOptionValue::asRole))
                                .map(Role::getId)
                                .flatMap(roleId -> Mono.defer(() -> {
                                    adminConfig.muteRoleId(roleId);
                                    return messageService.text(env.event(), "command.settings.mute-role.update",
                                            DiscordUtil.getRoleMention(roleId))
                                            .and(entityRetriever.save(adminConfig));
                                }));

                        Mono<Void> adminRolesCommand = Mono.justOrEmpty(group.getOption("admin-roles"))
                                .switchIfEmpty(muteRoleCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                        .flatMap(ApplicationCommandInteractionOption::getValue))
                                        .map(ApplicationCommandInteractionOptionValue::asString)
                                        .filter(str -> !str.equalsIgnoreCase("help"))
                                        .switchIfEmpty(messageService.text(env.event(), "command.settings.admin-roles.current",
                                                Optional.of(formatCollection(adminConfig.adminRoleIds(), DiscordUtil::getRoleMention))
                                                        .filter(s -> !s.isBlank())
                                                        .orElse(messageService.get(env.context(), "command.settings.absents")))
                                                .then(Mono.empty()))
                                        .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                                .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                                .map(ApplicationCommandInteractionOptionValue::asString)))
                                .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                    boolean add = choice.equalsIgnoreCase("add");

                                    List<String> toHelp = new ArrayList<>();
                                    List<Snowflake> removed = new ArrayList<>();
                                    Set<Snowflake> roleIds = adminConfig.adminRoleIds();
                                    String[] text = enums.split("(\\s+)?,(\\s+)?");
                                    Mono<Void> fetch = Flux.fromArray(text)
                                            .flatMap(str -> env.event().getInteraction().getGuild()
                                            .flatMapMany(Guild::getRoles)
                                            .filter(role -> MessageUtil.parseRoleId(str) != null &&
                                                    role.getId().equals(MessageUtil.parseRoleId(str)))
                                            .doOnNext(role -> {
                                                if(add){
                                                    if(!roleIds.add(role.getId())){
                                                        toHelp.add(messageService.format(env.context(),
                                                                "command.settings.admin-roles.already-set", str));
                                                    }
                                                }else{
                                                    if(!roleIds.remove(role.getId())){
                                                        toHelp.add(messageService.format(env.context(),
                                                                "command.settings.admin-roles.already-remove", str));
                                                    }else{
                                                        removed.add(role.getId());
                                                    }
                                                }
                                            })
                                            .switchIfEmpty(Mono.fromRunnable(() -> toHelp.add(messageService.format(env.context(),
                                                    "command.settings.admin-roles.unknown", str)))))
                                            .then();

                                    return fetch.then(Mono.defer(() -> {
                                        if(toHelp.isEmpty()){
                                            adminConfig.adminRoleIds(roleIds);
                                            if(add){
                                                return messageService.text(env.event(), "command.settings.added",
                                                        formatCollection(roleIds, DiscordUtil::getRoleMention));
                                            }
                                            return messageService.text(env.event(), "command.settings.removed",
                                                    formatCollection(removed, DiscordUtil::getRoleMention));
                                        }else{
                                            String response = toHelp.stream().map(s -> " • " + s + "\n").collect(Collectors.joining());
                                            return messageService.error(env.event(), "command.settings.admin-roles.conflicted.title", response);
                                        }
                                    }));
                                }))).and(entityRetriever.save(adminConfig));

                        Function<Duration, String> formatDuration = duration -> PeriodFormat.wordBased(env.context().get(KEY_LOCALE))
                                .print(duration.toPeriod());

                        Mono<Void> warnDelayCommand = Mono.justOrEmpty(group.getOption("warn-delay"))
                                .switchIfEmpty(adminRolesCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)))
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.warn-delay.current",
                                        formatDuration.apply(adminConfig.warnExpireDelay())).then(Mono.empty()))
                                .flatMap(str -> Mono.defer(() -> {
                                    Duration duration = Optional.ofNullable(MessageUtil.parseDuration(str))
                                            .map(jduration -> Duration.millis(jduration.toMillis()))
                                            .orElse(null);
                                    if(duration == null){
                                        return messageService.err(env.event(), "command.settings.incorrect-duration");
                                    }

                                    adminConfig.warnExpireDelay(duration);
                                    return messageService.text(env.event(), "command.settings.warn-delay.update",
                                            formatDuration.apply(duration))
                                            .and(entityRetriever.save(adminConfig));
                                }));

                        return Mono.justOrEmpty(group.getOption("delay"))
                                .switchIfEmpty(warnDelayCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)))
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.base-delay.current",
                                        formatDuration.apply(adminConfig.muteBaseDelay())).then(Mono.empty()))
                                .flatMap(str -> Mono.defer(() -> {
                                    Duration duration = Optional.ofNullable(MessageUtil.parseDuration(str))
                                            .map(jduration -> Duration.millis(jduration.toMillis()))
                                            .orElse(null);
                                    if(duration == null){
                                        return messageService.err(env.event(), "command.settings.incorrect-duration");
                                    }

                                    adminConfig.muteBaseDelay(duration);
                                    return messageService.text(env.event(), "command.settings.base-delay.update",
                                            formatDuration.apply(duration))
                                            .and(entityRetriever.save(adminConfig));
                                }));
                    }));

            Mono<Void> handleAudit = Mono.justOrEmpty(env.event().getInteraction().getCommandInteraction()
                    .getOption("audit"))
                    .switchIfEmpty(handleAdmin.then(Mono.empty()))
                    .zipWith(entityRetriever.getAuditConfigById(guildId)
                            .switchIfEmpty(entityRetriever.createAuditConfig(guildId)))
                    .flatMap(function((group, auditConfig) -> {
                        Mono<Void> channelCommand = Mono.justOrEmpty(group.getOption("channel")
                                .flatMap(command -> command.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.channel.current",
                                        auditConfig.logChannelId().map(DiscordUtil::getChannelMention)
                                                .orElse(messageService.get(env.context(), "command.settings.absent")))
                                        .then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .flatMap(ApplicationCommandInteractionOptionValue::asChannel))
                                .map(Channel::getId)
                                .flatMap(channelId -> Mono.defer(() -> {
                                    auditConfig.logChannelId(channelId);
                                    return messageService.text(env.event(), "command.settings.channel.update",
                                            DiscordUtil.getChannelMention(channelId))
                                            .and(entityRetriever.save(auditConfig));
                                }));

                        Mono<Void> actionsCommand = Mono.justOrEmpty(group.getOption("actions"))
                                .switchIfEmpty(channelCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                        .flatMap(ApplicationCommandInteractionOption::getValue))
                                        .map(ApplicationCommandInteractionOptionValue::asString)
                                        .filter(str -> !str.equalsIgnoreCase("help"))
                                        .switchIfEmpty(messageService.text(env.event(), "command.settings.actions.all", formatCollection(
                                                EnumSet.allOf(AuditActionType.class),
                                                type -> messageService.getEnum(env.context(), type)))
                                                .then(Mono.empty()))
                                        .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                                .switchIfEmpty(messageService.text(env.event(), "command.settings.actions.current",
                                                        formatCollection(auditConfig.enabled(), type ->
                                                                messageService.getEnum(env.context(), type)))
                                                        .then(Mono.empty()))
                                                .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                                .map(ApplicationCommandInteractionOptionValue::asString)))
                                .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                    List<Tuple2<AuditActionType, String>> all = Arrays.stream(AuditActionType.values())
                                            .map(type -> Tuples.of(type, messageService.getEnum(env.context(), type)))
                                            .collect(Collectors.toUnmodifiableList());

                                    List<String> onlyNames = all.stream().map(Tuple2::getT2)
                                            .collect(Collectors.toUnmodifiableList());

                                    boolean add = choice.equalsIgnoreCase("add");

                                    Set<String> toHelp = new HashSet<>();
                                    Set<String> removed = new HashSet<>();
                                    Set<AuditActionType> flags = auditConfig.enabled();
                                    if(enums.equalsIgnoreCase("all")){
                                        if(add){
                                            flags.addAll(all.stream().map(Tuple2::getT1).collect(Collectors.toSet()));
                                        }else{
                                            flags.clear();
                                        }
                                    }else{
                                        String[] text = enums.split("(\\s+)?,(\\s+)?");
                                        for(String s : text){
                                            all.stream().filter(predicate((type, str) -> str.equalsIgnoreCase(s)))
                                                    .findFirst()
                                                    .ifPresentOrElse(consumer((type, str) -> {
                                                        if(add){
                                                            if(!flags.add(type)){
                                                                toHelp.add(messageService.format(env.context(),
                                                                        "command.settings.actions.already-set", s));
                                                            }
                                                        }else{
                                                            if(!flags.remove(type)){
                                                                toHelp.add(messageService.format(env.context(),
                                                                        "command.settings.actions.already-remove", s));
                                                            }else{
                                                                removed.add(str);
                                                            }
                                                        }
                                                    }), () -> {
                                                        String suggest = Strings.findClosest(onlyNames, s);
                                                        String response = suggest != null ? messageService.format(env.context(),
                                                            "command.settings.actions.unknown.suggest", s, suggest) :
                                                                messageService.format(env.context(), "command.settings.actions.unknown", s);
                                                        toHelp.add(response);
                                                    });
                                        }
                                    }

                                    if(toHelp.isEmpty()){
                                        auditConfig.enabled(flags);
                                        if(add){
                                            String formatted = flags.stream()
                                                    .map(type -> messageService.getEnum(env.context(), type))
                                                    .collect(Collectors.joining(", "));

                                            return messageService.text(env.event(), "command.settings.added", formatted);
                                        }

                                        return messageService.text(env.event(), "command.settings.removed",
                                                String.join(", ", removed));
                                    }else{
                                        String response = toHelp.stream().map(s -> " • " + s + "\n").collect(Collectors.joining());
                                        return messageService.error(env.event(), "command.settings.actions.conflicted.title", response);
                                    }
                                }))).and(entityRetriever.save(auditConfig));

                        Function<Boolean, String> formatBool = bool ->
                                messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

                        return Mono.justOrEmpty(group.getOption("enable"))
                                .switchIfEmpty(actionsCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)))
                                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.enable.update",
                                        formatBool.apply(auditConfig.isEnable())).then(Mono.empty()))
                                .flatMap(bool -> Mono.defer(() -> {
                                    auditConfig.setEnable(bool);
                                    return messageService.text(env.event(), "command.settings.enable.update", formatBool.apply(bool))
                                            .and(entityRetriever.save(auditConfig));
                                }));
                    }));

            return Mono.justOrEmpty(env.event().getInteraction().getCommandInteraction()
                    .getOption("common"))
                    .switchIfEmpty(handleAudit.then(Mono.empty()))
                    .zipWith(entityRetriever.getGuildConfigById(guildId)
                            .switchIfEmpty(entityRetriever.createGuildConfig(guildId)))
                    .flatMap(function((group, guildConfig) -> {
                        Mono<Void> timezoneCommand = Mono.justOrEmpty(group.getOption("timezone")
                                .flatMap(command -> command.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.timezone.current",
                                        guildConfig.timeZone()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    DateTimeZone timeZone = Commands.TimezoneCommand.findTimeZone(str);
                                    if(timeZone == null){
                                        String suggest = Strings.findClosest(DateTimeZone.getAvailableIDs(), str);

                                        if(suggest != null){
                                            return messageService.err(env.event(), "command.settings.timezone.unknown.suggest", suggest);
                                        }
                                        return messageService.err(env.event(), "command.settings.timezone.unknown");
                                    }

                                    guildConfig.timeZone(timeZone);
                                    return Mono.deferContextual(ctx -> messageService.text(env.event(),
                                            "command.settings.timezone.update", ctx.<Locale>get(KEY_TIMEZONE)))
                                            .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone))
                                            .and(entityRetriever.save(guildConfig));
                                }));

                        Mono<Void> localeCommand = Mono.justOrEmpty(group.getOption("locale"))
                                .switchIfEmpty(timezoneCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.locale.current",
                                        guildConfig.locale()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    Locale locale = LocaleUtil.get(str);
                                    if(locale == null){
                                        String all = formatCollection(LocaleUtil.locales.values(), Locale::toString);
                                        return messageService.text(env.event(), "command.settings.locale.all", all);
                                    }

                                    guildConfig.locale(locale);
                                    return Mono.deferContextual(ctx -> messageService.text(env.event(), "command.settings.locale.update",
                                            ctx.<Locale>get(KEY_LOCALE)))
                                            .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale))
                                            .and(entityRetriever.save(guildConfig));
                                }));

                        return Mono.justOrEmpty(group.getOption("prefix"))
                                .switchIfEmpty(localeCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.settings.prefix.current",
                                        guildConfig.prefix()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    guildConfig.prefix(str);
                                    return messageService.text(env.event(), "command.settings.prefix.update", guildConfig.prefix())
                                            .and(entityRetriever.save(guildConfig));
                                }));

                    }));
        }

        private <T> String formatCollection(Collection<? extends T> collection, Function<T, String> formatter){
            return collection.stream()
                    .map(formatter)
                    .collect(Collectors.joining(", "));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("settings")
                    .description("Configure guild settings.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("common")
                            .description("Different bot settings")
                            .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("prefix")
                                    .description("Configure bot prefix")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New prefix")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("locale")
                                    .description("Configure bot locale")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New locale")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("timezone")
                                    .description("Configure bot time zone")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New time zone")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .build())
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
                                                    .name("Add audit action type")
                                                    .value("add")
                                                    .build())
                                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                    .name("Remove audit action type")
                                                    .value("remove")
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
                                    .name("delay")
                                    .description("Configure default delay")
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
                                    .name("warn-delay")
                                    .description("Configure warn expire delay")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("Duration")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
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
                                                    .name("Add admin role")
                                                    .value("add")
                                                    .build())
                                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                    .name("Remove admin role")
                                                    .value("remove")
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

    @InteractionDiscordCommand
    public static class TextLayoutCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            boolean russian = env.event().getInteraction().getCommandInteraction()
                    .getOption("type")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map("ru"::equalsIgnoreCase)
                    .orElse(false);

            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(str -> russian ? Commands.TextLayoutCommand.text2eng(str) : Commands.TextLayoutCommand.text2rus(str))
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("r")
                    .description("Change text layout.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Text layout type")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("English layout")
                                    .value("en")
                                    .build())
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("Russian layout")
                                    .value("ru")
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Target text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class LeetSpeakCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            boolean russian = env.event().getInteraction().getCommandInteraction()
                    .getOption("type")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map("ru"::equalsIgnoreCase)
                    .orElse(false);

            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(str -> Commands.LeetSpeakCommand.leeted(str, russian))
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("1337")
                    .description("Translate text into leet speak.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Leet speak type")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("English leet")
                                    .value("en")
                                    .build())
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("Russian leet")
                                    .value("ru")
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Target text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class TransliterationCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(Commands.TransliterationCommand::translit)
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("tr")
                    .description("Translating text into transliteration.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Translation text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class DeleteCommand extends AdminCommand{
        @Autowired
        private AuditService auditService;

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Member author = env.event().getInteraction().getMember()
                    .orElseThrow(AssertionError::new);

            Mono<TextChannel> reply = env.getReplyChannel().cast(TextChannel.class);

            long number = env.event().getInteraction().getCommandInteraction()
                    .getOption("count")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .orElse(0L);

            if(number <= 0){
                return messageService.err(env.event(), "command.incorrect-number");
            }

            if(number > settings.getDiscord().getMaxClearedCount()){
                return messageService.err(env.event(), "common.limit-number", settings.getDiscord().getMaxClearedCount());
            }

            StringBuffer result = new StringBuffer();
            Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                    .withLocale(env.context().get(KEY_LOCALE))
                    .withZone(env.context().get(KEY_TIMEZONE));

            BiConsumer<Message, Member> appendInfo = (message, member) -> {
                result.append("[").append(formatter.print(message.getTimestamp().toEpochMilli())).append("] ");
                if(DiscordUtil.isBot(member)){
                    result.append("[BOT] ");
                }

                result.append(member.getUsername());
                member.getNickname().ifPresent(nickname -> result.append(" (").append(nickname).append(")"));
                result.append(" >");
                String content = MessageUtil.effectiveContent(message);
                if(!content.isBlank()){
                    result.append(" ").append(content);
                }
                if(!message.getEmbeds().isEmpty()){
                    result.append(" (... ").append(message.getEmbeds().size()).append(" embed(s))");
                }
                result.append("\n");
            };

            Mono<Void> history = reply.flatMapMany(channel -> channel.getLastMessage()
                    .flatMapMany(message -> channel.getMessagesBefore(message.getId()))
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .doOnNext(member -> appendInfo.accept(message, member))
                            .flatMap(ignored -> entityRetriever.deleteMessageInfoById(message.getId()))
                            .thenReturn(message))
                    .transform(messages -> number > 1 ? channel.bulkDeleteMessages(messages).then() : messages.next().flatMap(Message::delete).then()))
                    .then();

            Mono<Void> log = reply.flatMap(channel -> auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withChannel(channel)
                    .withAttribute(COUNT, number)
                    .withAttachment(MESSAGE_TXT, ReusableByteInputStream.ofString(result.toString()))
                    .save());

            return history.then(log).and(env.event().reply("\u2063✅")); // to reduce emoji size
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("delete")
                    .description("Delete some messages.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("count")
                            .description("How many messages to delete")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class AvatarCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            return env.event().getInteraction().getCommandInteraction()
                    .getOption("target")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asUser)
                    .orElse(Mono.just(env.event().getInteraction().getUser()))
                    .flatMap(user -> messageService.info(env.event(), embed -> embed.setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(env.context(), "command.avatar.text", user.getUsername(),
                                    DiscordUtil.getUserMention(user.getId())))));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("avatar")
                    .description("Get user avatar.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("target")
                            .description("Whose avatar needs to get. By default your avatar")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class PingCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Mono<MessageChannel> reply = env.getReplyChannel();

            long start = System.currentTimeMillis();
            return env.event().acknowledge().then(env.event().getInteractionResponse()
                    .createFollowupMessage(messageService.get(env.context(), "command.ping.testing"))
                    .flatMap(data -> reply.flatMap(channel -> channel.getMessageById(Snowflake.of(data.id())))
                            .flatMap(message -> message.edit(spec -> spec.setContent(messageService.format(env.context(), "command.ping.completed",
                                    System.currentTimeMillis() - start))))))
                    .then();
        }
        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("ping")
                    .description("Get bot ping.")
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class MathCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            String expression = env.event().getInteraction().getCommandInteraction()
                    .getOption("expression")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<BigDecimal> result = Mono.fromCallable(() -> {
                Expression exp = new Expression(expression);
                exp.addOperator(Commands.MathCommand.shiftRightOperator);
                exp.addOperator(Commands.MathCommand.shiftLeftOperator);
                return exp.eval();
            });

            return result.publishOn(Schedulers.boundedElastic())
                    .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException,
                    t -> messageService.error(env.event(), "command.math.error.title", t.getMessage()).then(Mono.empty()))
                    .flatMap(decimal -> messageService.text(env.event(), MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("math")
                    .description("Calculate math expression.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("expression")
                            .description("Math expression")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }
}
