package inside.common.command.model;

import arc.util.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.common.command.model.base.*;
import inside.common.command.service.CommandHandler;
import inside.common.services.DiscordService;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.*;

@Service
public class Commands{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private GuildService guildService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CommandHandler handler;

    @Autowired
    private Settings settings;

    @DiscordCommand(key = "help", description = "command.help.description")
    public class HelpCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            StringBuffer builder = new StringBuffer();
            Snowflake guildId = reference.member().getGuildId();
            String prefix = guildService.prefix(guildId);

            handler.commandList().forEach(command -> {
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0){
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append('*');
                }
                builder.append(" - ");
                builder.append(messageService.get(command.description));
                builder.append('\n');
            });

            return messageService.info(event.getMessage().getChannel(), messageService.get("command.help"), builder.toString());
        }
    }

    @DiscordCommand(key = "ping", description = "command.ping.description")
    public class PingCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            long now = System.currentTimeMillis();
            return messageService.text(event.getMessage().getChannel(), messageService.format("command.ping", System.currentTimeMillis() - now));
        }
    }

    // @DiscordCommand(key = "config", params = "[add/set/remove] [name] [value...]", description = "commands.config.description")
    // public class ConfigCommand extends CommandRunner{
    //     public final List<String> allowedKeys = List.of("guildID", "id");
    //
    //     @Override
    //     public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
    //         Mono<MessageChannel> channel = event.getMessage().getChannel();
    //         if(!adminService.isOwner(reference.member())){
    //             return messageService.err(channel, messageService.get("command.owner-only"));
    //         }
    //
    //         GuildConfig config = guildService.get(reference.member().getGuildId());
    //
    //         if(args.length == 0){
    //             StringBuilder builder = new StringBuilder();
    //             builder.append("prefix: ").append(config.prefix()).append("\n");
    //             builder.append("locale: ").append(config.locale()).append("\n");
    //             builder.append("logging: ").append(config.logChannelId() != null).append("\n");
    //             builder.append("active user system: ").append(config.activeUserRoleID() != null).append("\n");
    //             builder.append("mute system: ").append(config.muteRoleID() != null).append("\n");
    //             builder.append("admined roles: ").append(config.adminRoleIdsAsList().toString().replaceAll("\\[\\]", ""));
    //             return messageService.info(channel, "all", builder.toString());
    //         }
    //
    //         ObjectNode node = (ObjectNode)JacksonUtil.toJsonNode(config);
    //         String name = args[1];
    //
    //         if(name != null && node.has(name) && !allowedKeys.contains(name)){
    //             if(args[0].equals("remove")){
    //                 node.remove(name);
    //                 return messageService.text(channel, "@ removed", name);
    //             }
    //
    //             if(args.length == 2){
    //                 return messageService.text(channel, "@ = @", name, node.get(name));
    //             }
    //
    //             String value = args[2];
    //             JsonNode valueNode = node.get(name);
    //
    //             if(args[0].equals("add")){
    //                  if(valueNode instanceof ArrayNode arrayNode){
    //                      if(MessageUtil.parseRoleId(value) == null){
    //                          return messageService.text(channel, "@ not a role id", value);
    //                      }
    //                      arrayNode.add(value);
    //                  }else{
    //                      return messageService.text(channel, "@ not array", value);
    //                  }
    //             }else if(args[0].equals("set")){
    //                 if(valueNode instanceof TextNode){
    //                     node.set(name, new TextNode(value));
    //                     return messageService.text(channel, "@ set to @", name, value);
    //                 }else{
    //                     return messageService.text(channel, "@ not a string", value);
    //                 }
    //             }else{
    //                 return messageService.text(channel, "Not found action @", args[0]);
    //             }
    //         }else{
    //             return messageService.text(channel, "Not found property @", name);
    //         }
    //
    //         GuildConfig now = JacksonUtil.fromString(node.toString(), GuildConfig.class);
    //         return messageService.text(channel, "all = @", node).then(Mono.fromRunnable(() -> guildService.save(now)));
    //     }
    // }

    @DiscordCommand(key = "prefix", params = "[prefix]", description = "command.config.prefix.description")
    public class PrefixCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Guild guild = event.getGuild().block();
            Mono<MessageChannel> channel = event.getMessage().getChannel();

            GuildConfig c = guildService.get(guild);
            if(args.length == 0){
                return messageService.text(channel, messageService.format("command.config.prefix", c.prefix()));
            }else{
                if(!adminService.isOwner(reference.member())){
                    return messageService.err(channel, messageService.get("command.owner-only"));
                }

                if(!MessageUtil.isEmpty(args[0])){
                    c.prefix(args[0]);
                    guildService.save(c);
                    return messageService.text(channel, messageService.format("command.config.prefix-updated", c.prefix()));
                }
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "locale", params = "[locale]", description = "command.config.locale.description")
    public class LocaleCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Guild guild = event.getGuild().block();
            Mono<MessageChannel> channel = event.getMessage().getChannel();

            GuildConfig c = guildService.get(guild);
            Supplier<String> r = () -> context.locale().equals(Locale.ROOT) ? messageService.get("common.default") : context.locale().toString();
            if(args.length == 0){
                return messageService.text(channel, messageService.format("command.config.locale", r.get()));
            }else{
                if(!adminService.isOwner(reference.member())){
                    return messageService.err(channel, messageService.get("command.owner-only"));
                }

                if(!MessageUtil.isEmpty(args[0])){
                    Locale l = LocaleUtil.getOrDefault(args[0]);
                    c.locale(l);
                    guildService.save(c);
                    context.locale(l);
                    return messageService.text(channel, messageService.format("command.config.locale-updated", r.get()));
                }
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "mute", params = "<@user> <delay> [reason...]", description = "command.admin.mute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class MuteCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Mono<MessageChannel> channel = event.getMessage().getChannel();
            if(!MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            try{
                int delayDays = Strings.parseInt(args[1]);
                LocalMember info = memberService.get(reference.member().getGuildId(), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.user().userId()).block();
                String reason = args.length > 2 ? args[2] : null;

                if(DiscordUtil.isBot(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-bot"));
                }

                if(adminService.isMuted(m.getGuildId(), m.getId()).blockOptional().orElse(false)){
                    return messageService.err(channel, messageService.get("command.admin.mute.already-muted"));
                }

                if(adminService.isAdmin(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-admin"));
                }

                if(reference.member().equals(m)){
                    return messageService.err(channel, messageService.get("command.admin.mute.self-user"));
                }

                if(reason != null && !reason.isBlank() && reason.length() > 1000){
                    return messageService.err(channel, messageService.format("common.string-limit", 1000));
                }

                return Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberMuteEvent(m.getGuild().block(), reference.localMember(), info, reason, delayDays)));
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }

    @DiscordCommand(key = "delete", params = "<amount>", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public class DeleteCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Mono<TextChannel> channel = event.getMessage().getChannel().cast(TextChannel.class);
            if(!MessageUtil.canParseInt(args[0])){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            int number = Integer.parseInt(args[0]);
            if(number >= 100){
                return messageService.err(channel, messageService.format("common.limit-number", 100));
            }

            List<Message> history = channel.flatMapMany(c -> c.getMessagesBefore(event.getMessage().getId()))
                                           .limitRequest(number)
                                           .collectList()
                                           .block();

            if(history == null || (history.isEmpty() && number > 0)){
                return messageService.err(channel, messageService.get("message.error.history-retrieve"));
            }

            discordService.eventListener().publish(new MessageClearEvent(event.getGuild().block(), history, reference.user(), channel.block(), number));
            return Mono.empty();
        }
    }

    @DiscordCommand(key = "warn", params = "<@user> [reason...]", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public class WarnCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){ //todo переделать предложение
            Mono<MessageChannel> channel = event.getMessage().getChannel();
            try{
                LocalMember info = memberService.get(reference.member().getGuildId(), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.user().userId()).block();
                String reason = args.length > 1 ? args[1] : null;

                if(DiscordUtil.isBot(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-bot"));
                }

                if(adminService.isAdmin(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-admin"));
                }

                if(reference.member().equals(m)){
                    return messageService.err(channel, messageService.get("command.admin.warn.self-user"));
                }

                if(reason != null && !reason.isBlank() && reason.length() > 1000){
                    return messageService.err(channel, messageService.format("common.string-limit", 1000));
                }

                adminService.warn(reference.localMember(), info, reason).block();
                long warnings = adminService.warnings(m.getGuildId(), info.user().userId()).count().blockOptional().orElse(0L);

                messageService.text(channel, messageService.format("message.admin.warn", m.getUsername(), warnings)).block();

                if(warnings >= 3){
                    return event.getGuild().flatMap(g -> g.ban(m.getId(), b -> b.setDeleteMessageDays(0)));
                }
                return Mono.empty();
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.admin.warnings.description")
    public class WarningsCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Mono<MessageChannel> channel = event.getMessage().getChannel();
            try{
                LocalMember info = memberService.get(reference.member().getGuildId(), MessageUtil.parseUserId(args[0]));
                List<AdminAction> warns = adminService.warnings(info.guildId(), info.user().userId()).limitRequest(21)
                                                      .collectList().blockOptional().orElse(Collections.emptyList());
                if(warns.isEmpty()){
                    return messageService.text(channel, messageService.get("command.admin.warnings.empty"));
                }else{
                    DateTimeFormatter formatter = DateTimeFormat.shortDateTime();
                    Consumer<EmbedCreateSpec> spec = e -> {
                        e.setColor(settings.normalColor);
                        e.setTitle(messageService.format("command.admin.warnings.title", info.effectiveName()));
                        for(int i = 0; i < warns.size(); i++){
                            AdminAction w = warns.get(i);
                            String title = String.format("%2s. %s", i + 1, formatter.print(new DateTime(w.timestamp())));

                            StringBuilder description = new StringBuilder();
                            description.append(messageService.format("common.admin", w.admin().effectiveName())).append('\n');
                            description.append(messageService.format("common.reason", w.reason().orElse(messageService.get("common.not-defined"))));
                            e.addField(title, description.toString(), true);
                        }
                    };
                    return messageService.info(channel, spec);
                }
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }

    @DiscordCommand(key = "unwarn", params = "<@user> [number]", description = "command.admin.unwarn.description")
    public class UnwarnCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Mono<MessageChannel> channel = event.getMessage().getChannel();
            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            int warnings = args.length > 1 ? Strings.parseInt(args[1]) + 1 : 0;

            try{
                LocalMember info = memberService.get(reference.member().getGuildId(), MessageUtil.parseUserId(args[0]));
                adminService.unwarn(info.guildId(), info.user().userId(), warnings).block();
                return messageService.text(channel, messageService.format("command.admin.unwarn", info.effectiveName(), warnings + 1));
            }catch(Throwable t){
                if(t instanceof IndexOutOfBoundsException){
                    return messageService.err(channel, messageService.get("command.incorrect-number"));
                }else{
                    return messageService.err(channel, messageService.get("command.incorrect-name"));
                }
            }
        }
    }

    @DiscordCommand(key = "unmute", params = "<@user>", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class UnmuteCommand extends CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            Mono<MessageChannel> channel = event.getMessage().getChannel();
            try{
                LocalMember member = memberService.get(reference.member().getGuildId(), MessageUtil.parseUserId(args[0]));
                if(!adminService.isMuted(member.guildId(), member.userId()).blockOptional().orElse(false)){
                    return messageService.err(channel, messageService.format("audit.member.unmute.is-not-muted", member.username()));
                }
                return Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberUnmuteEvent(event.getGuild().block(), member)));
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }
}
