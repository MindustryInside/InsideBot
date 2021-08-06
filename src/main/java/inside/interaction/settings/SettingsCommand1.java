package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.audit.AuditActionType;
import inside.data.entity.*;
import inside.interaction.InteractionCommandEnvironment;
import inside.util.*;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.*;
import reactor.util.function.*;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

@Deprecated(forRemoval = true)
// @InteractionDiscordCommand(name = "settings", description = "Configure guild settings.")
public class SettingsCommand1 extends OwnerCommand{
    private static final Pattern unicode = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", Pattern.UNICODE_CHARACTER_CLASS);

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

        Mono<Void> handleStarboard = Mono.justOrEmpty(env.event().getOption("starboard"))
                .zipWith(entityRetriever.getStarboardConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createStarboardConfig(guildId)))
                .flatMap(function((group, starboardConfig) -> {
                    Function<List<EmojiData>, String> formatEmojis = emojis -> {
                        StringBuilder builder = new StringBuilder();
                        int lastnceil = 0;
                        for(EmojiData data : emojis){
                            builder.append(lastnceil).append("..").append(lastnceil + 5);
                            builder.append(" - ");
                            builder.append(DiscordUtil.getEmojiString(data));
                            builder.append("\n");
                            lastnceil += 5;
                        }
                        return builder.toString();
                    };

                    Mono<Void> emojisCommand = Mono.justOrEmpty(group.getOption("emojis"))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                            .flatMap(ApplicationCommandInteractionOption::getValue))
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .filter(str -> !str.equals("help"))
                                    .switchIfEmpty(messageService.text(env.event(), "command.settings.emojis.current",
                                                    formatEmojis.apply(starboardConfig.emojis()))
                                            .then(Mono.empty()))
                                    .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                            .switchIfEmpty(messageService.text(env.event(), "command.settings.emojis.current",
                                                            formatEmojis.apply(starboardConfig.emojis()))
                                                    .then(Mono.empty()))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                List<EmojiData> emojis = starboardConfig.emojis();
                                if(choice.equals("clear")){
                                    emojis.clear();
                                    return messageService.text(env.event(), "command.settings.emojis.clear");
                                }

                                boolean add = choice.equals("add");

                                if(enums.matches("^(#\\d+)$") && !add){ // index mode
                                    String str = enums.substring(1);
                                    if(!MessageUtil.canParseInt(str)){
                                        return messageService.err(env.event(), "command.settings.emojis.overflow-index")
                                                .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                    }

                                    int idx = Strings.parseInt(str) - 1; // Counting the index from 1
                                    if(idx < 0 || idx >= emojis.size()){
                                        return messageService.err(env.event(), "command.settings.emojis.index-out-of-bounds")
                                                .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                    }

                                    EmojiData value = emojis.remove(idx);
                                    return messageService.text(env.event(), "command.settings.removed",
                                            DiscordUtil.getEmojiString(value));
                                }

                                String[] text = enums.split("(\\s+)?,(\\s+)?");
                                return Flux.fromArray(text).flatMap(str -> env.getClient().getGuildEmojis(guildId)
                                                .filter(emoji -> emoji.asFormat().equals(str) || emoji.getName().equals(str) ||
                                                        emoji.getId().asString().equals(str))
                                                .map(GuildEmoji::getData)
                                                .switchIfEmpty(Mono.just(str)
                                                        .filter(s -> unicode.matcher(s).find())
                                                        .map(s -> EmojiData.builder()
                                                                .name(s)
                                                                .build())))
                                        .collectList()
                                        .flatMap(list -> {
                                            var tmp = new ArrayList<>(emojis);
                                            if(add){
                                                tmp.addAll(list);
                                                if(tmp.size() > 20){
                                                    return messageService.err(env.event(), "command.settings.emojis.limit")
                                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                                }

                                                emojis.addAll(list);
                                            }else{
                                                tmp.removeAll(list);
                                                if(tmp.size() < 1){
                                                    return messageService.err(env.event(), "command.settings.emojis.no-emojis")
                                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                                }

                                                emojis.removeAll(list);
                                            }

                                            if(add){
                                                return messageService.text(env.event(), "command.settings.added",
                                                        formatCollection(list, DiscordUtil::getEmojiString));
                                            }
                                            return messageService.text(env.event(), "command.settings.removed",
                                                    formatCollection(list, DiscordUtil::getEmojiString));
                                        });
                            }).and(entityRetriever.save(starboardConfig))));

                    Mono<Void> lowerStarBarrierCommand = Mono.justOrEmpty(group.getOption("lower-star-barrier"))
                            .switchIfEmpty(emojisCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.lower-star-barrier.current",
                                    starboardConfig.lowerStarBarrier()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.negative-number")
                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true)).then(Mono.empty()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env.event(), "command.settings.overflow-number")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                starboardConfig.lowerStarBarrier(i);
                                return messageService.text(env.event(), "command.settings.lower-star-barrier.update", i)
                                        .and(entityRetriever.save(starboardConfig));
                            });

                    Mono<Void> channelCommand = Mono.justOrEmpty(group.getOption("channel"))
                            .switchIfEmpty(lowerStarBarrierCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.starboard-channel.current",
                                            starboardConfig.starboardChannelId().map(DiscordUtil::getChannelMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asChannel))
                            .map(Channel::getId)
                            .flatMap(channelId -> {
                                starboardConfig.starboardChannelId(channelId);
                                return messageService.text(env.event(), "command.settings.starboard-channel.update",
                                                DiscordUtil.getChannelMention(channelId))
                                        .and(entityRetriever.save(starboardConfig));
                            });

                    return Mono.justOrEmpty(group.getOption("enable"))
                            .switchIfEmpty(channelCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asBoolean)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.starboard-enable.update",
                                    formatBool.apply(starboardConfig.isEnabled())).then(Mono.empty()))
                            .flatMap(bool -> {
                                starboardConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.starboard-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(starboardConfig));
                            });
                }));

        Mono<Void> handleActivities = Mono.justOrEmpty(env.event().getOption("activities"))
                .switchIfEmpty(handleStarboard.then(Mono.empty()))
                .zipWith(entityRetriever.getActivityConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createActiveUserConfig(guildId)))
                .flatMap(function((group, activityConfig) -> {
                    Mono<Void> keepCountingDurationCommand = Mono.justOrEmpty(group.getOption("duration"))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.keep-counting-duration.current",
                                    formatDuration.apply(activityConfig.keepCountingDuration())).then(Mono.empty()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                activityConfig.keepCountingDuration(duration);
                                return messageService.text(env.event(), "command.settings.keep-counting-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(activityConfig));
                            });

                    Mono<Void> messageBarrierCommand = Mono.justOrEmpty(group.getOption("message-barrier"))
                            .switchIfEmpty(keepCountingDurationCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.message-barrier.current",
                                    activityConfig.messageBarrier()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .filter(l -> l > 0)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.negative-number")
                                    .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true)).then(Mono.empty()))
                            .flatMap(l -> {
                                int i = (int)(long)l;
                                if(i != l){
                                    return messageService.err(env.event(), "command.settings.overflow-number")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                activityConfig.messageBarrier(i);
                                return messageService.text(env.event(), "command.settings.message-barrier.update", i)
                                        .and(entityRetriever.save(activityConfig));
                            });

                    Mono<Void> activeUserRoleCommand = Mono.justOrEmpty(group.getOption("active-user-role"))
                            .switchIfEmpty(messageBarrierCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.active-user-role.current",
                                            activityConfig.roleId().map(DiscordUtil::getRoleMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asRole))
                            .map(Role::getId)
                            .flatMap(roleId -> {
                                activityConfig.roleId(roleId);
                                return messageService.text(env.event(), "command.settings.active-user-role.update",
                                                DiscordUtil.getRoleMention(roleId))
                                        .and(entityRetriever.save(activityConfig));
                            });

                    return Mono.justOrEmpty(group.getOption("enable"))
                            .switchIfEmpty(activeUserRoleCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)))
                            .map(ApplicationCommandInteractionOptionValue::asBoolean)
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.activities-enable.update",
                                    formatBool.apply(activityConfig.isEnabled())).then(Mono.empty()))
                            .flatMap(bool -> {
                                activityConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.activities-enable.update", formatBool.apply(bool))
                                        .and(entityRetriever.save(activityConfig));
                            });
                }));

        Mono<Void> handleAdmin = Mono.justOrEmpty(env.event().getOption("admin"))
                .switchIfEmpty(handleActivities.then(Mono.empty()))
                .zipWith(entityRetriever.getAdminConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAdminConfig(guildId)))
                .flatMap(function((group, adminConfig) -> {
                    Mono<Void> warnThresholdActionCommand = Mono.justOrEmpty(group.getOption("threshold-action")
                                    .flatMap(opt -> opt.getOption("value"))
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.threshold-action.current",
                                    String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.thresholdAction()),
                                            adminConfig.thresholdAction())).then(Mono.empty()))
                            .flatMap(str -> {
                                AdminActionType action = Try.ofCallable(() ->
                                        AdminActionType.valueOf(str)).toOptional().orElse(null);
                                Objects.requireNonNull(action, "action"); // impossible
                                adminConfig.thresholdAction(action);

                                return messageService.text(env.event(), "command.settings.threshold-action.update",
                                                String.format("%s (`%s`)", messageService.getEnum(env.context(), adminConfig.thresholdAction()),
                                                        adminConfig.thresholdAction()))
                                        .and(entityRetriever.save(adminConfig));
                            });

                    Mono<Void> warningsCommand = Mono.justOrEmpty(group.getOption("warnings"))
                            .switchIfEmpty(warnThresholdActionCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.warnings.current",
                                    adminConfig.maxWarnCount()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asLong))
                            .flatMap(number -> {
                                adminConfig.maxWarnCount(number);
                                return messageService.text(env.event(), "command.settings.warnings.update", number)
                                        .and(entityRetriever.save(adminConfig));
                            });

                    Mono<Void> muteRoleCommand = Mono.justOrEmpty(group.getOption("mute-role"))
                            .switchIfEmpty(warningsCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.mute-role.current",
                                            adminConfig.muteRoleID().map(DiscordUtil::getRoleMention)
                                                    .orElseGet(() -> messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asRole))
                            .map(Role::getId)
                            .flatMap(roleId -> {
                                adminConfig.muteRoleId(roleId);
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
                                                    Optional.of(formatCollection(adminConfig.adminRoleIds(), DiscordUtil::getRoleMention))
                                                            .filter(s -> !s.isBlank())
                                                            .orElseGet(() -> messageService.get(env.context(), "command.settings.absents")))
                                            .then(Mono.empty()))
                                    .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                Set<Snowflake> roleIds = adminConfig.adminRoleIds();
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
                                    adminConfig.adminRoleIds(roleIds);
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
                                    formatDuration.apply(adminConfig.warnExpireDelay())).then(Mono.empty()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                adminConfig.warnExpireDelay(duration);
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
                                    formatDuration.apply(adminConfig.muteBaseDelay())).then(Mono.empty()))
                            .flatMap(str -> {
                                Duration duration = Try.ofCallable(() -> MessageUtil.parseDuration(str)).orElse(null);
                                if(duration == null){
                                    return messageService.err(env.event(), "command.settings.incorrect-duration")
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                adminConfig.muteBaseDelay(duration);
                                return messageService.text(env.event(), "command.settings.base-duration.update",
                                                formatDuration.apply(duration))
                                        .and(entityRetriever.save(adminConfig));
                            });
                }));

        Mono<Void> handleAudit = Mono.justOrEmpty(env.event().getOption("audit"))
                .switchIfEmpty(handleAdmin.then(Mono.empty()))
                .zipWith(entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId)))
                .flatMap(function((group, auditConfig) -> {
                    Mono<Void> channelCommand = Mono.justOrEmpty(group.getOption("channel")
                                    .flatMap(command -> command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.log-channel.current",
                                            auditConfig.logChannelId().map(DiscordUtil::getChannelMention)
                                                    .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .flatMap(ApplicationCommandInteractionOptionValue::asChannel))
                            .map(Channel::getId)
                            .flatMap(channelId -> {
                                auditConfig.logChannelId(channelId);
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
                                                            formatCollection(auditConfig.types(), type ->
                                                                    messageService.getEnum(env.context(), type)))
                                                    .then(Mono.empty()))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                Set<AuditActionType> flags = auditConfig.types();
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

        return Mono.justOrEmpty(env.event().getOption("common"))
                .switchIfEmpty(handleAudit.then(Mono.empty()))
                .zipWith(entityRetriever.getGuildConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createGuildConfig(guildId)))
                .flatMap(function((group, guildConfig) -> {
                    Mono<Void> reactionRolesCommand = Mono.justOrEmpty(group.getOption("reaction-roles"))
                            .flatMap(opt -> Mono.zip(Mono.justOrEmpty(opt.getOption("type")
                                                    .flatMap(ApplicationCommandInteractionOption::getValue))
                                            .map(ApplicationCommandInteractionOptionValue::asString),
                                    Mono.just(opt.getOption("message-id")
                                            .flatMap(ApplicationCommandInteractionOption::getValue)
                                            .map(ApplicationCommandInteractionOptionValue::asString)),
                                    Mono.just(opt.getOption("emoji")
                                            .flatMap(ApplicationCommandInteractionOption::getValue)
                                            .map(ApplicationCommandInteractionOptionValue::asString)),
                                    Mono.just(opt.getOption("role")
                                            .flatMap(ApplicationCommandInteractionOption::getValue)
                                            .map(ApplicationCommandInteractionOptionValue::asSnowflake))))
                            .flatMap(function((choice, messageId, emojistr, role) -> {
                                if(choice.equals("clear")){
                                    return entityRetriever.deleteAllEmojiDispenserInGuild(guildId)
                                            .then(messageService.text(env.event(), "command.settings.reaction-roles.clear"));
                                }

                                Function<EmojiDispenser, String> formatEmojiDispenser = e -> String.format("%s -> %s (%s)\n",
                                        e.messageId().asString(), DiscordUtil.getRoleMention(e.roleId()),
                                        DiscordUtil.getEmojiString(e.emoji()));

                                if(choice.equals("help")){
                                    return entityRetriever.getAllEmojiDispenserInGuild(guildId)
                                            .map(formatEmojiDispenser)
                                            .collect(Collectors.joining())
                                            .flatMap(str -> messageService.text(env.event(),
                                                    "command.settings.reaction-roles.current", str));
                                }

                                Mono<Snowflake> preparedRoleId = Mono.justOrEmpty(role)
                                        .switchIfEmpty(messageService.text(env.event(),
                                                "command.settings.reaction-roles.role-absent").then(Mono.empty()));

                                Mono<Snowflake> preparedMessageId = Mono.justOrEmpty(messageId)
                                        .switchIfEmpty(messageService.text(env.event(),
                                                "command.settings.reaction-roles.message-id-absent").then(Mono.empty()))
                                        .mapNotNull(MessageUtil::parseId) // it's safe
                                        .switchIfEmpty(messageService.text(env.event(),
                                                "command.settings.reaction-roles.incorrect-message-id").then(Mono.empty()));

                                if(choice.equals("add")){
                                    Mono<EmojiData> fetchEmoji = Mono.justOrEmpty(emojistr)
                                            .switchIfEmpty(messageService.text(env.event(),
                                                    "command.settings.reaction-roles.emoji-absent").then(Mono.empty()))
                                            .flatMap(str -> env.getClient().getGuildEmojis(guildId)
                                                    .filter(emoji -> emoji.asFormat().equals(str) || emoji.getName().equals(str) ||
                                                            emoji.getId().asString().equals(str))
                                                    .map(GuildEmoji::getData)
                                                    .defaultIfEmpty(EmojiData.builder()
                                                            .name(str)
                                                            .build())
                                                    .next());

                                    return Mono.zip(preparedMessageId, fetchEmoji, preparedRoleId)
                                            .filterWhen(ignored -> entityRetriever.getEmojiDispenserCountInGuild(guildId)
                                                    .map(l -> l < 20))
                                            .switchIfEmpty(messageService.text(env.event(),
                                                    "command.settings.reaction-roles.limit").then(Mono.empty()))
                                            .flatMap(function((id, emoji, roleId) -> entityRetriever.createEmojiDispenser(guildId, id, roleId, emoji)
                                                    .flatMap(emojiDispenser -> messageService.text(env.event(), "command.settings.added",
                                                            formatEmojiDispenser.apply(emojiDispenser)))));
                                }

                                return Mono.zip(preparedMessageId, preparedRoleId)
                                        .flatMap(function(entityRetriever::getEmojiDispenserById))
                                        .switchIfEmpty(messageService.text(env.event(),
                                                "command.settings.reaction-roles.not-found").then(Mono.empty()))
                                        .flatMap(emojiDispenser -> entityRetriever.delete(emojiDispenser)
                                                .thenReturn(emojiDispenser))
                                        .flatMap(emojiDispenser -> messageService.text(env.event(), "command.settings.removed",
                                                formatEmojiDispenser.apply(emojiDispenser)));
                            }));

                    Mono<Void> timezoneCommand = Mono.justOrEmpty(group.getOption("timezone"))
                            .switchIfEmpty(reactionRolesCommand.then(Mono.empty()))
                            .flatMap(command -> Mono.justOrEmpty(command.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.timezone.current",
                                    guildConfig.timeZone()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .flatMap(str -> {
                                ZoneId timeZone = Try.ofCallable(() -> ZoneId.of(str)).orElse(null);
                                if(timeZone == null){
                                    return ZoneId.getAvailableZoneIds().stream()
                                            .min(Comparator.comparingInt(s -> Strings.levenshtein(s, str)))
                                            .map(s -> messageService.err(env.event(), "command.settings.timezone.unknown.suggest", s))
                                            .orElse(messageService.err(env.event(), "command.settings.timezone.unknown"))
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                guildConfig.timeZone(timeZone);
                                return Mono.deferContextual(ctx -> messageService.text(env.event(),
                                                "command.settings.timezone.update", ctx.<Locale>get(KEY_TIMEZONE)))
                                        .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone))
                                        .and(entityRetriever.save(guildConfig));
                            });

                    Mono<Void> localeCommand = Mono.justOrEmpty(group.getOption("locale"))
                            .switchIfEmpty(timezoneCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.locale.current",
                                    guildConfig.locale().getDisplayName()).then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                    .map(ApplicationCommandInteractionOptionValue::asString))
                            .flatMap(str -> {
                                Locale locale = messageService.getLocale(str).orElse(null);
                                if(locale == null){
                                    String all = formatCollection(messageService.getSupportedLocales().values(), locale1 ->
                                            "%s (`%s`)".formatted(locale1.getDisplayName(), locale1.toString()));
                                    return messageService.text(env.event(), "command.settings.locale.all", all)
                                            .contextWrite(ctx -> ctx.put(KEY_EPHEMERAL, true));
                                }

                                guildConfig.locale(locale);
                                return Mono.deferContextual(ctx -> messageService.text(env.event(), "command.settings.locale.update",
                                                ctx.<Locale>get(KEY_LOCALE).getDisplayName()))
                                        .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale))
                                        .and(entityRetriever.save(guildConfig));
                            });

                    return Mono.justOrEmpty(group.getOption("prefix"))
                            .switchIfEmpty(localeCommand.then(Mono.empty()))
                            .flatMap(opt -> Mono.justOrEmpty(opt.getOption("type")
                                            .flatMap(ApplicationCommandInteractionOption::getValue))
                                    .map(ApplicationCommandInteractionOptionValue::asString)
                                    .filter(str -> !str.equals("help"))
                                    .switchIfEmpty(messageService.text(env.event(), "command.settings.prefix.current",
                                                    String.join(", ", guildConfig.prefixes()))
                                            .then(Mono.empty()))
                                    .zipWith(Mono.justOrEmpty(opt.getOption("value"))
                                            .switchIfEmpty(messageService.text(env.event(), "command.settings.prefix.current",
                                                            String.join(", ", guildConfig.prefixes()))
                                                    .then(Mono.empty()))
                                            .flatMap(subopt -> Mono.justOrEmpty(subopt.getValue()))
                                            .map(ApplicationCommandInteractionOptionValue::asString)))
                            .flatMap(function((choice, enums) -> Mono.defer(() -> {
                                List<String> flags = guildConfig.prefixes();
                                if(choice.equals("clear")){
                                    flags.clear();
                                    return messageService.text(env.event(), "command.settings.prefix.clear");
                                }

                                boolean add = choice.equals("add");

                                List<String> removed = new ArrayList<>(0);
                                String[] text = enums.split("(\\s+)?,(\\s+)?");
                                for(String s : text){
                                    if(add){
                                        flags.add(s);
                                    }else{
                                        if(flags.remove(s)){
                                            removed.add(s);
                                        }
                                    }
                                }

                                if(add){
                                    return messageService.text(env.event(), "command.settings.added",
                                            String.join(", ", flags));
                                }
                                return messageService.text(env.event(), "command.settings.removed",
                                        String.join(", ", removed));
                            }).and(entityRetriever.save(guildConfig))));

                }));
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
                                .description("Configure bot prefixes")
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
                                                .name("Add prefix(s)")
                                                .value("add")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove prefix(s)")
                                                .value("remove")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove all prefixes")
                                                .value("clear")
                                                .build())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Target prefix(s)")
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
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("reaction-roles")
                                .description("Configure reaction roles")
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
                                                .name("Add reaction role")
                                                .value("add")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove reaction role")
                                                .value("remove")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove all reaction roles")
                                                .value("clear")
                                                .build())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("message-id")
                                        .description("Target message id")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("emoji")
                                        .description("Target emoji")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("role")
                                        .description("Target role")
                                        .type(ApplicationCommandOptionType.ROLE.getValue())
                                        .build())
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("activities")
                        .description("Activity features settings")
                        .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("enable")
                                .description("Enable activity features")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Boolean value")
                                        .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("active-user-role")
                                .description("Configure active user role")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Active user role")
                                        .type(ApplicationCommandOptionType.ROLE.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("message-barrier")
                                .description("Configure message barrier")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Minimal message count for reward activity")
                                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("keep-counting-duration")
                                .description("Configure keep counting duration")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Period value")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("starboard")
                        .description("Starboard settings")
                        .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("enable")
                                .description("Enable starboard")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Boolean value")
                                        .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("lower-star-barrier")
                                .description("Set star barrier")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Integer value")
                                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("emojis")
                                .description("Configure starboard emojis")
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
                                                .name("Add emoji(s)")
                                                .value("add")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove emoji(s)")
                                                .value("remove")
                                                .build())
                                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                                .name("Remove all emojis")
                                                .value("clear")
                                                .build())
                                        .build())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Target emoji(s)")
                                        .type(ApplicationCommandOptionType.STRING.getValue())
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("channel")
                                .description("Configure starboard channel")
                                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("value")
                                        .description("Starboard channel")
                                        .type(ApplicationCommandOptionType.CHANNEL.getValue())
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
