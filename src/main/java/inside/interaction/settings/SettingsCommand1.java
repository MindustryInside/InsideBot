package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.audit.AuditActionType;
import inside.interaction.InteractionCommandEnvironment;
import inside.util.DiscordUtil;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.Mono;
import reactor.util.function.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.*;

@Deprecated(forRemoval = true)
public class SettingsCommand1 extends OwnerCommand{

    private SettingsCommand1(){
        super(null);
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        Snowflake guildId = env.event().getInteraction().getGuildId()
                .orElseThrow(IllegalStateException::new);

        BooleanFunction<String> formatBool = bool ->
                messageService.get(env.context(), bool ? "command.settings.enabled" : "command.settings.disabled");

        return Mono.justOrEmpty(env.event().getOption("audit"))
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
                                                            ((Collection<? extends AuditActionType>)auditConfig.getTypes()).stream()
                                                                    .map((Function<AuditActionType, String>)type ->
                                                                            messageService.getEnum(env.context(), type))
                                                                    .collect(Collectors.joining(", ")))
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
                                    String formatted = ((Collection<? extends AuditActionType>)flags).stream()
                                            .map((Function<AuditActionType, String>)type ->
                                                    messageService.getEnum(env.context(), type))
                                            .collect(Collectors.joining(", "));

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
                .build();
    }
}
