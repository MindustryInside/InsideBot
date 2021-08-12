package inside.interaction.settings;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.audit.AuditActionType;
import inside.interaction.*;
import inside.util.DiscordUtil;
import inside.util.func.BooleanFunction;
import reactor.core.publisher.Mono;
import reactor.util.function.*;

import java.util.*;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.*;

@InteractionDiscordCommand(name = "audit", description = "Audit log settings.")
public class AuditCommand extends OwnerCommand{

    protected AuditCommand(@Aware List<? extends InteractionOwnerAwareCommand<AuditCommand>> subcommands){
        super(subcommands);
    }

    @InteractionDiscordCommand(name = "enable", description = "Enable audit logging.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AuditCommandEnable extends OwnerAwareCommand<AuditCommand>{

        protected AuditCommandEnable(@Aware AuditCommand owner){
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

            return entityRetriever.getAuditConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                    .flatMap(auditConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.audit-enable.current",
                                    formatBool.apply(auditConfig.isEnabled())).then(Mono.never()))
                            .flatMap(bool -> {
                                auditConfig.setEnabled(bool);
                                return messageService.text(env.event(), "command.settings.audit-enable.update",
                                                formatBool.apply(bool))
                                        .and(entityRetriever.save(auditConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "channel", description = "Configure log channel.",
            type = ApplicationCommandOptionType.SUB_COMMAND)
    public static class AuditCommandChannel extends OwnerAwareCommand<AuditCommand>{

        protected AuditCommandChannel(@Aware AuditCommand owner){
            super(owner);

            addOption(builder -> builder.name("value")
                    .description("New log channel.")
                    .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        }

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){

            Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

            return entityRetriever.getAuditConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                    .flatMap(auditConfig -> Mono.justOrEmpty(env.getOption("value")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asSnowflake))
                            .switchIfEmpty(messageService.text(env.event(), "command.settings.log-channel.current",
                                    auditConfig.getLogChannelId().map(DiscordUtil::getChannelMention)
                                            .orElse(messageService.get(env.context(), "command.settings.absent")))
                                    .then(Mono.never()))
                            .flatMap(channelId -> {
                                auditConfig.setLogChannelId(channelId);
                                return messageService.text(env.event(), "command.settings.log-channel.update",
                                                DiscordUtil.getChannelMention(channelId))
                                        .and(entityRetriever.save(auditConfig));
                            }));
        }
    }

    @InteractionDiscordCommand(name = "actions", description = "Configure bot locale.",
            type = ApplicationCommandOptionType.SUB_COMMAND_GROUP)
    public static class AuditCommandActions extends SubGroupOwnerCommand<AuditCommand>{

        protected AuditCommandActions(@Aware AuditCommand owner, @Aware List<? extends InteractionOwnerAwareCommand<AuditCommandActions>> subcommands){
            super(owner, subcommands);
        }

        @InteractionDiscordCommand(name = "help", description = "Get a help.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AuditCommandActionsHelp extends OwnerAwareCommand<AuditCommandActions>{

            protected AuditCommandActionsHelp(@Aware AuditCommandActions owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                        .flatMap(auditConfig -> messageService.text(env.event(), "command.settings.actions.current",
                                auditConfig.getTypes().stream()
                                        .map(type -> messageService.getEnum(env.context(), type))
                                        .collect(Collectors.joining(", "))));
            }
        }

        @InteractionDiscordCommand(name = "add", description = "Add audit action(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AuditCommandActionsAdd extends OwnerAwareCommand<AuditCommandActions>{

            protected AuditCommandActionsAdd(@Aware AuditCommandActions owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("New audit action.")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                        .flatMap(auditConfig -> Mono.justOrEmpty(env.getOption("value")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<AuditActionType> flags = auditConfig.getTypes();

                                    List<Tuple2<AuditActionType, String>> all = Arrays.stream(AuditActionType.all)
                                            .map(type -> Tuples.of(type, messageService.getEnum(env.context(), type)))
                                            .toList();

                                    if(value.equalsIgnoreCase("all")){
                                        flags.addAll(all.stream().map(Tuple2::getT1).collect(Collectors.toSet()));
                                    }else{
                                        String[] text = value.split("(\\s+)?,(\\s+)?");
                                        Arrays.stream(text).forEach(s -> all.stream()
                                                .filter(predicate((type, str) -> str.equalsIgnoreCase(s)))
                                                .findFirst()
                                                .ifPresent(consumer((type, str) -> flags.add(type))));
                                    }

                                    return messageService.text(env.event(), "command.settings.added", flags.stream()
                                                    .map(type -> messageService.getEnum(env.context(), type))
                                                    .collect(Collectors.joining(", ")))
                                            .and(entityRetriever.save(auditConfig));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "remove", description = "Remove audit action(s).",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AuditCommandActionsRemove extends OwnerAwareCommand<AuditCommandActions>{

            protected AuditCommandActionsRemove(@Aware AuditCommandActions owner){
                super(owner);

                addOption(builder -> builder.name("value")
                        .description("Audit action.")
                        .required(true)
                        .type(ApplicationCommandOptionType.STRING.getValue()));
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                        .flatMap(auditConfig -> Mono.justOrEmpty(env.getOption("value")
                                        .flatMap(ApplicationCommandInteractionOption::getValue)
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(value -> {
                                    Set<AuditActionType> flags = auditConfig.getTypes();

                                    Set<String> removed = new HashSet<>();
                                    List<Tuple2<AuditActionType, String>> all = Arrays.stream(AuditActionType.all)
                                            .map(type -> Tuples.of(type, messageService.getEnum(env.context(), type)))
                                            .toList();

                                    String[] text = value.split("(\\s+)?,(\\s+)?");
                                    Arrays.stream(text).forEach(s -> all.stream()
                                            .filter(predicate((type, str) -> str.equalsIgnoreCase(s)))
                                            .findFirst()
                                            .ifPresent(consumer((type, str) -> {
                                                if(flags.remove(type)){
                                                    removed.add(str);
                                                }
                                            })));

                                    return messageService.text(env.event(), "command.settings.removed",
                                            String.join(", ", removed))
                                            .and(entityRetriever.save(auditConfig));
                                }));
            }
        }

        @InteractionDiscordCommand(name = "clear", description = "Remove all audit actions.",
                type = ApplicationCommandOptionType.SUB_COMMAND)
        public static class AuditCommandActionsClear extends OwnerAwareCommand<AuditCommandActions>{

            protected AuditCommandActionsClear(@Aware AuditCommandActions owner){
                super(owner);
            }

            @Override
            public Mono<Void> execute(InteractionCommandEnvironment env){

                Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow(IllegalStateException::new);

                return entityRetriever.getAuditConfigById(guildId)
                        .switchIfEmpty(entityRetriever.createAuditConfig(guildId))
                        .flatMap(auditConfig -> {
                            auditConfig.getTypes().clear();
                            return messageService.text(env.event(), "command.settings.actions.clear")
                                    .and(entityRetriever.save(auditConfig));
                        });
            }
        }
    }
}
