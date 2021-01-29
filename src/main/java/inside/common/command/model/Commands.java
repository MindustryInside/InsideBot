package inside.common.command.model;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.common.command.model.base.*;
import inside.common.command.service.CommandHandler;
import inside.data.service.DiscordService;
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
import reactor.util.context.Context;

import java.util.*;
import java.util.function.*;

import static inside.util.ContextUtil.*;

@Service
public class Commands{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private CommandHandler handler;

    @Autowired
    private Settings settings;

    @DiscordCommand(key = "help", description = "command.help.description")
    public class HelpCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            StringBuffer builder = new StringBuffer();
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();
            String prefix = entityRetriever.prefix(guildId);

            handler.commandList().forEach(command -> {
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0){
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(messageService.get(ref.context(), command.description));
                builder.append("\n");
            });
            builder.append(messageService.get(ref.context(), "command.help.disclaimer.user"));

            return messageService.info(ref.getReplyChannel(), messageService.get(ref.context(), "command.help"), builder.toString());
        }
    }

    @DiscordCommand(key = "avatar", params = "[@user]", description = "command.avatar.description")
    public class AvatarCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = args.length > 0 ? MessageUtil.parseUserId(args[0]) : ref.getAuthorAsMember().getId();
            if(targetId == null || !discordService.exists(targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            return discordService.gateway().getUserById(targetId).flatMap(user -> messageService.info(channel, embed -> {
                embed.setColor(settings.normalColor);
                embed.setImage(user.getAvatarUrl() + "?size=512");
                embed.setDescription(messageService.format(ref.context(), "command.avatar.text", user.getUsername()));
            }));
        }
    }

    @DiscordCommand(key = "ping", description = "command.ping.description")
    public class PingCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return Mono.just(System.currentTimeMillis())
                    .timestamp().flatMap(t ->
                            messageService.text(ref.getReplyChannel(), messageService.format(ref.context(), "command.ping", t.getT1() - t.getT2()))
                    );
        }
    }

    @DiscordCommand(key = "1337", params = "<text...>", description = "command.1337.description")
    public class LeetCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.leeted(args[0]));
        }
    }

    @DiscordCommand(key = "tr", params = "<text...>", description = "command.translit.description")
    public class TranslitCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.translit(args[0]));
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
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.event().getMessage().getChannel();

            return member.getGuild().map(guild -> entityRetriever.getGuild(guild))
                    .flatMap(guildConfig -> {
                        if(args.length == 0){
                            return messageService.text(channel, messageService.format(ref.context(), "command.config.prefix", guildConfig.prefix()));
                        }else{
                            if(!adminService.isOwner(member)){
                                return messageService.err(channel, messageService.get(ref.context(), "command.owner-only"));
                            }

                            if(!MessageUtil.isEmpty(args[0])){
                                guildConfig.prefix(args[0]);
                                entityRetriever.save(guildConfig);
                                return messageService.text(channel, messageService.format(ref.context(), "command.config.prefix-updated", guildConfig.prefix()));
                            }
                        }

                        return Mono.empty();
                    });
        }
    }

    @DiscordCommand(key = "locale", params = "[locale]", description = "command.config.locale.description")
    public class LocaleCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();

            return member.getGuild().map(guild -> entityRetriever.getGuild(guild))
                    .flatMap(guildConfig -> {
                        Supplier<String> r = () -> Objects.equals(ref.context().get(KEY_LOCALE), Locale.ROOT) ? messageService.get(ref.context(), "common.default") : ref.context().get(KEY_LOCALE).toString();

                        if(args.length == 0){
                            return messageService.text(channel, messageService.format(ref.context(), "command.config.locale", r.get()));
                        }else{
                            if(!adminService.isOwner(member)){
                                return messageService.err(channel, messageService.get(ref.context(), "command.owner-only"));
                            }

                            if(!MessageUtil.isEmpty(args[0])){
                                Locale locale = LocaleUtil.get(args[0]);
                                if(locale == null){
                                    String all = Strings.join("\n", LocaleUtil.locales.values().toSeq().map(Locale::toString));
                                    return messageService.text(channel, messageService.format(ref.context(), "command.config.unknown", all));
                                }

                                guildConfig.locale(locale);
                                entityRetriever.save(guildConfig);
                                ((Context)ref.context()).put(KEY_LOCALE, locale);
                                return messageService.text(channel, messageService.format(ref.context(), "command.config.locale-updated", r.get()));
                            }
                        }

                        return Mono.empty();
                    });
        }
    }

    @DiscordCommand(key = "mute", params = "<@user> <delay> [reason...]", description = "command.admin.mute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class MuteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();

            Member author = ref.getAuthorAsMember();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();
            if(targetId == null || !discordService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            DateTime delay = MessageUtil.parseTime(args[1]);
            if(delay == null){
                return messageService.err(channel, messageService.get(ref.context(), "message.error.invalid-time"));
            }

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target, () -> new LocalMember(target)))
                            .filterWhen(local -> adminService.isMuted(guildId, target.getId())
                                    .flatMap(b -> b ? messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.already-muted")).then(Mono.just(false)) : Mono.just(true)))
                            .flatMap(local -> {
                                String reason = args.length > 2 ? args[2].trim() : null;

                                if(adminService.isAdmin(target) && !adminService.isOwner(author)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin"));
                                }

                                if(Objects.equals(author, target)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.self-user"));
                                }

                                if(reason != null && !reason.isBlank() && reason.length() > 1000){
                                    return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 1000));
                                }

                                return Mono.fromRunnable(() -> discordService.eventListener().publish(
                                        new MemberMuteEvent(target.getGuild().block(), ref.localMember(), local, reason, delay)
                                ));
                            })
            );
        }
    }

    @DiscordCommand(key = "delete", params = "<amount>", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public class DeleteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<TextChannel> channel = ref.getReplyChannel().cast(TextChannel.class);
            if(!MessageUtil.canParseInt(args[0])){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
            }

            int number = Strings.parseInt(args[0]);
            if(number >= 100){
                return messageService.err(channel, messageService.format(ref.context(), "common.limit-number", 100));
            }

            Flux<Message> history = channel.flatMapMany(c -> c.getMessagesBefore(ref.getMessage().getId()))
                                           .limitRequest(number);

            return author.getGuild().flatMap(g -> Mono.fromRunnable(() -> discordService.eventListener().publish(
                    new MessageClearEvent(g, history, author, channel, number)
            )));
        }
    }

    @DiscordCommand(key = "warn", params = "<@user> [reason...]", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public class WarnCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();
            if(targetId == null || !discordService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target, () -> new LocalMember(target)))
                             .flatMap(local -> {
                                 String reason = args.length > 1 ? args[1].trim() : null;

                                 if(adminService.isAdmin(target) && !adminService.isOwner(author)){
                                     return messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin"));
                                 }

                                 if(Objects.equals(author, target)){
                                     return messageService.err(channel, messageService.get(ref.context(), "command.admin.warn.self-user"));
                                 }

                                 if(!MessageUtil.isEmpty(reason) && reason.length() >= 1000){
                                     return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 1000));
                                 }

                                 return adminService.warn(ref.localMember(), local, reason)
                                        .then(Mono.defer(() -> adminService.warnings(local.guildId(), local.userId()).count())
                                                .flatMap(count -> {
                                                    Mono<Void> publisher = messageService.text(channel, messageService.format(ref.context(), "message.admin.warn", target.getUsername(), count));

                                                    if(count >= 3){
                                                        return publisher.then(author.getGuild().flatMap(g -> g.ban(target.getId(), b -> b.setDeleteMessageDays(0))));
                                                    }
                                                    return publisher;
                                                }));
                             })
                    );
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.admin.warnings.description")
    public class WarningsCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();
            if(targetId == null || !discordService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target, () -> new LocalMember(target)))
                            .flatMap(local -> {
                                Flux<AdminAction> warnings = adminService.warnings(local.guildId(), local.userId()).limitRequest(21);

                                return warnings.hasElements().flatMap(b -> !b ? messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty")) :
                                        Mono.defer(() -> messageService.info(channel, embed ->
                                            warnings.index().subscribe(t -> {
                                                DateTimeFormatter formatter = DateTimeFormat
                                                        .shortDateTime()
                                                        .withLocale(ref.context().get(KEY_LOCALE))
                                                        .withZone(ref.context().get(KEY_TIMEZONE));

                                                AdminAction warn = t.getT2();
                                                embed.setColor(settings.normalColor);
                                                embed.setTitle(messageService.format(ref.context(), "command.admin.warnings.title", local.effectiveName()));
                                                String title = String.format("%2s. %s", t.getT1() + 1, formatter.print(new DateTime(warn.timestamp())));

                                                String description = String.format("%s%n%s",
                                                messageService.format(ref.context(), "common.admin", warn.admin().effectiveName()),
                                                messageService.format(ref.context(), "common.reason", warn.reason().orElse(messageService.get(ref.context(), "common.not-defined"))));
                                                embed.addField(title, description, true);
                                            })
                                        )));
                            }));
        }
    }

    @DiscordCommand(key = "unwarn", params = "<@user> [number]", description = "command.admin.unwarn.description")
    public class UnwarnCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();
            if(targetId == null || !discordService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
            }

            return adminService.get(AdminActionType.warn, guildId, targetId).count().flatMap(count -> {
                int warn = args.length > 1 ? Strings.parseInt(args[1]) : 1;
                if(count == 0){
                    return messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty"));
                }

                if(warn > count){
                    return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
                }

                return discordService.gateway().getMemberById(guildId, targetId)
                        .map(target -> entityRetriever.getMember(target, () -> new LocalMember(target)))
                                .flatMap(local -> messageService.text(channel, messageService.format(ref.context(), "command.admin.unwarn", local.effectiveName(), warn))
                                        .then(adminService.unwarn(local.guildId(), local.userId(), warn - 1)));
            });
        }
    }

    @DiscordCommand(key = "unmute", params = "<@user>", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class UnmuteCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.localMember().guildId();
            if(targetId == null || !discordService.exists(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            Mono<Member> target = discordService.gateway().getMemberById(guildId, targetId);

            return target.map(member -> entityRetriever.getMember(member, () -> new LocalMember(member)))
                    .flatMap(local -> adminService.isMuted(local.guildId(), local.userId())
                            .flatMap(bool -> bool ? ref.event()
                                    .getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberUnmuteEvent(guild, local))))
                                            : target.flatMap(member -> messageService.err(channel, messageService.format(ref.context(), "audit.member.unmute.is-not-muted", member.getUsername())))
                            )
                    );
        }
    }
}
