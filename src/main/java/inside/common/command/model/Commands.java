package inside.common.command.model;

import arc.util.Strings;
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
import inside.data.service.AdminService.AdminActionType;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

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
    public class HelpCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            StringBuffer builder = new StringBuffer();
            Snowflake guildId = reference.getAuthorAsMember().getGuildId();
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
            builder.append(messageService.get("command.help.disclaimer.user"));

            return messageService.info(reference.getReplyChannel(), messageService.get("command.help"), builder.toString());
        }
    }

    @DiscordCommand(key = "avatar", params = "<@user>", description = "command.avatar.description")
    public class AvatarCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Mono<MessageChannel> channel = reference.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            if(targetId == null || !discordService.exists(targetId)){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }

            return discordService.gateway().getUserById(targetId).flatMap(u -> messageService.info(channel, e -> {
                e.setColor(settings.normalColor);
                e.setImage(u.getAvatarUrl() + "?size=512");
                e.setDescription(messageService.format("command.avatar.text", u.getUsername()));
            }));
        }
    }

    @DiscordCommand(key = "ping", description = "command.ping.description")
    public class PingCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            long now = System.currentTimeMillis();
            return messageService.text(reference.getReplyChannel(), messageService.format("command.ping", System.currentTimeMillis() - now));
        }
    }

    // @DiscordCommand(key = "config", params = "[add/set/remove] [name] [value...]", description = "commands.config.description")
    // public class ConfigCommand extends CommandRunner{
    //     public final List<String> allowedKeys = List.of("guildID", "id");
    //
    //     @Override
    //     public Mono<Void> execute(CommandReference reference, String[] args){
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
    public class PrefixCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Guild guild = reference.event().getGuild().block();
            Mono<MessageChannel> channel = reference.event().getMessage().getChannel();

            GuildConfig c = guildService.get(guild);
            if(args.length == 0){
                return messageService.text(channel, messageService.format("command.config.prefix", c.prefix()));
            }else{
                if(!adminService.isOwner(reference.getAuthorAsMember())){
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
    public class LocaleCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Member member = reference.getAuthorAsMember();
            Guild guild = member.getGuild().block();
            Mono<MessageChannel> channel = reference.getReplyChannel();

            GuildConfig c = guildService.get(guild);
            Supplier<String> r = () -> Objects.equals(context.locale(), Locale.ROOT) ? messageService.get("common.default") : context.locale().toString();
            if(args.length == 0){
                return messageService.text(channel, messageService.format("command.config.locale", r.get()));
            }else{
                if(!adminService.isOwner(member)){
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
    public class MuteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Mono<MessageChannel> channel = reference.getReplyChannel();

            try{
                Member author = reference.getAuthorAsMember();
                DateTime delay = MessageUtil.parseTime(args[1]);
                if(delay == null){
                    return messageService.err(channel, messageService.get("message.error.invalid-time"));
                }
                LocalMember info = memberService.get(author.getGuildId(), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.user().userId()).block();
                String reason = args.length > 2 ? args[2].trim() : null;

                if(DiscordUtil.isBot(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-bot"));
                }

                if(adminService.isMuted(m.getGuildId(), m.getId()).blockOptional().orElse(false)){
                    return messageService.err(channel, messageService.get("command.admin.mute.already-muted"));
                }

                if(adminService.isAdmin(m) && !adminService.isOwner(author)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-admin"));
                }

                if(Objects.equals(author, m)){
                    return messageService.err(channel, messageService.get("command.admin.mute.self-user"));
                }

                if(reason != null && !reason.isBlank() && reason.length() > 1000){
                    return messageService.err(channel, messageService.format("common.string-limit", 1000));
                }

                return Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberMuteEvent(m.getGuild().block(), reference.localMember(), info, reason, delay)));
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }

    @DiscordCommand(key = "delete", params = "<amount>", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public class DeleteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Member member = reference.getAuthorAsMember();
            Mono<TextChannel> channel = reference.getReplyChannel().cast(TextChannel.class);
            if(!MessageUtil.canParseInt(args[0])){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            int number = Strings.parseInt(args[0]);
            if(number >= 100){
                return messageService.err(channel, messageService.format("common.limit-number", 100));
            }

            Flux<Message> history = channel.flatMapMany(c -> c.getMessagesBefore(reference.getMessage().getId()))
                                           .limitRequest(number);

            return member.getGuild().flatMap(g -> Mono.fromRunnable(() -> discordService.eventListener().publish(new MessageClearEvent(g, history, member, channel, number))));
        }
    }

    @DiscordCommand(key = "warn", params = "<@user> [reason...]", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public class WarnCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){ //todo переделать предложение
            Member author = reference.getAuthorAsMember();
            Mono<MessageChannel> channel = reference.getReplyChannel();
            try{
                LocalMember info = memberService.get(author.getGuildId(), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.user().userId()).block();
                String reason = args.length > 1 ? args[1].trim() : null;

                if(DiscordUtil.isBot(m)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-bot"));
                }

                if(adminService.isAdmin(m) && !adminService.isOwner(author)){
                    return messageService.err(channel, messageService.get("command.admin.user-is-admin"));
                }

                if(Objects.equals(author, m)){
                    return messageService.err(channel, messageService.get("command.admin.warn.self-user"));
                }

                if(reason != null && !reason.isBlank() && reason.length() > 1000){
                    return messageService.err(channel, messageService.format("common.string-limit", 1000));
                }

                adminService.warn(reference.localMember(), info, reason).block();
                long warnings = adminService.warnings(m.getGuildId(), info.user().userId()).count().blockOptional().orElse(0L);

                Mono<Void> publisher = messageService.text(channel, messageService.format("message.admin.warn", m.getUsername(), warnings));

                if(warnings >= 3){
                    return publisher.then(author.getGuild().flatMap(g -> g.ban(m.getId(), b -> b.setDeleteMessageDays(0))));
                }
                return publisher;
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.admin.warnings.description")
    public class WarningsCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Mono<MessageChannel> channel = reference.getReplyChannel();
            try{
                LocalMember info = memberService.get(reference.getAuthorAsMember().getGuildId(), MessageUtil.parseUserId(args[0]));
                List<AdminAction> warns = adminService.warnings(info.guildId(), info.user().userId()).limitRequest(21)
                                                      .collectList().blockOptional().orElse(Collections.emptyList());
                if(warns.isEmpty()){
                    return messageService.text(channel, messageService.get("command.admin.warnings.empty"));
                }else{
                    DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                                                                .withLocale(context.locale())
                                                                .withZone(context.zone());
                    Consumer<EmbedCreateSpec> spec = e -> {
                        e.setColor(settings.normalColor);
                        e.setTitle(messageService.format("command.admin.warnings.title", info.effectiveName()));
                        for(int i = 0; i < warns.size(); i++){
                            AdminAction w = warns.get(i);
                            String title = String.format("%2s. %s", i + 1, formatter.print(new DateTime(w.timestamp())));

                            String description = String.format("%s%n%s",
                            messageService.format("common.admin", w.admin().effectiveName()),
                            messageService.format("common.reason", w.reason().orElse(messageService.get("common.not-defined"))));
                            e.addField(title, description, true);
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
    public class UnwarnCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Mono<MessageChannel> channel = reference.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = reference.getAuthorAsMember().getGuildId();
            if(targetId == null || !discordService.exists(guildId, targetId) || !memberService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            long count = adminService.get(AdminActionType.warn, guildId, targetId).count().blockOptional().orElse(0L);
            int warn = args.length > 1 ? Strings.parseInt(args[1]) : 1;
            boolean under = warn > count;
            if(count == 0){
                return messageService.text(channel, messageService.get("command.admin.warnings.empty"));
            }
            if(under){
                return messageService.err(channel, messageService.get("command.incorrect-number"));
            }

            LocalMember info = memberService.get(guildId, targetId);
            return messageService.text(channel, messageService.format("command.admin.unwarn", info.effectiveName(), warn))
                                 .then(adminService.unwarn(info.guildId(), info.user().userId(), warn - 1));
        }
    }

    @DiscordCommand(key = "unmute", params = "<@user>", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class UnmuteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference reference, String[] args){
            Mono<MessageChannel> channel = reference.getReplyChannel();
            try{
                LocalMember member = memberService.get(reference.getAuthorAsMember().getGuildId(), MessageUtil.parseUserId(args[0]));
                if(!adminService.isMuted(member.guildId(), member.userId()).blockOptional().orElse(false)){
                    return messageService.err(channel, messageService.format("audit.member.unmute.is-not-muted", member.username()));
                }
                return reference.event().getGuild().flatMap(g -> Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberUnmuteEvent(g, member))));
            }catch(Exception e){
                return messageService.err(channel, messageService.get("command.incorrect-name"));
            }
        }
    }
}
