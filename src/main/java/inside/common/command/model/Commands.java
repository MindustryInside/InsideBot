package inside.common.command.model;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.common.Collector;
import inside.common.command.CommandHandler;
import inside.common.command.model.base.*;
import inside.data.entity.AdminAction;
import inside.data.service.*;
import inside.event.dispatcher.EventType.*;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.*;
import reactor.util.context.Context;

import java.util.*;
import java.util.function.Supplier;

import static inside.util.ContextUtil.*;

@Collector
public class Commands{
    private static final Logger log = Loggers.getLogger(Commands.class);

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

    public class ModeratorCommand extends Command{
        @Override
        public Mono<Boolean> apply(CommandRequest req){
            return adminService.isAdmin(req.getAuthorAsMember());
        }
    }

    @DiscordCommand(key = "help", description = "command.help.description")
    public class HelpCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            StringBuilder builder = new StringBuilder();
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();
            final String prefix = entityRetriever.prefix(guildId);

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
            if(targetId == null || !discordService.existsUserById(targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            return discordService.gateway().withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(targetId)
                    .flatMap(user -> messageService.info(channel, embed -> embed.setColor(settings.normalColor)
                            .setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(ref.context(), "command.avatar.text", user.getUsername()))));
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

    @DiscordCommand(key = "prefix", params = "[prefix]", description = "command.config.prefix.description")
    public class PrefixCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.event().getMessage().getChannel();

            return Mono.justOrEmpty(entityRetriever.getGuildById(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isOwner(member))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.owner-only")).then(Mono.empty()))
                    .flatMap(guildConfig -> {
                        if(args.length == 0){
                            return messageService.text(channel, messageService.format(ref.context(), "command.config.prefix", guildConfig.prefix()));
                        }else{
                            if(!args[0].isBlank()){
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

            return Mono.just(entityRetriever.getGuildById(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isOwner(member))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.owner-only")).then(Mono.empty()))
                    .flatMap(guildConfig -> {
                        Supplier<String> formatter = () -> ref.context().<Locale>getOrEmpty(KEY_LOCALE)
                                .filter(locale -> !locale.equals(Locale.ROOT))
                                .map(Locale::toString)
                                .orElse(messageService.get(ref.context(), "common.default"));

                        if(args.length == 0){
                            return messageService.text(channel, messageService.format(ref.context(), "command.config.locale", formatter.get()));
                        }else{
                            if(!args[0].isBlank()){
                                Locale locale = LocaleUtil.get(args[0]);
                                if(locale == null){
                                    String all = Strings.join(", ", LocaleUtil.locales.values().toSeq().map(Locale::toString));
                                    return messageService.text(channel, messageService.format(ref.context(), "command.config.unknown", all));
                                }

                                guildConfig.locale(locale);
                                entityRetriever.save(guildConfig);
                                ((Context)ref.context()).put(KEY_LOCALE, locale);
                                return messageService.text(channel, messageService.format(ref.context(), "command.config.locale-updated", formatter.get()));
                            }
                        }

                        return Mono.empty();
                    });
        }
    }

    @DiscordCommand(key = "mute", params = "<@user> <delay> [reason...]", description = "command.admin.mute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class MuteCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();

            Member author = ref.getAuthorAsMember();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();

            if(entityRetriever.muteRoleId(guildId).isEmpty()){
                return messageService.err(channel, messageService.get(ref.context(), "command.disabled.mute"));
            }

            if(targetId == null || !discordService.existsMemberById(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            DateTime delay = MessageUtil.parseTime(args[1]);
            if(delay == null){
                return messageService.err(channel, messageService.get(ref.context(), "message.error.invalid-time"));
            }

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target))
                            .filterWhen(local -> adminService.isMuted(guildId, target.getId()).map(bool -> !bool))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.already-muted")).then(Mono.never()))
                            .filterWhen(local -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author)).map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin")).then(Mono.empty()))
                            .flatMap(local -> Mono.defer(() -> {
                                String reason = args.length > 2 ? args[2].trim() : null;

                                if(Objects.equals(author, target)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.self-user"));
                                }

                                if(reason != null && !reason.isBlank() && reason.length() > 512){
                                    return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 512));
                                }

                                return target.getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(
                                        new MemberMuteEvent(guild, ref.localMember(), local, reason, delay)
                                )));
                            }))
                            .then());
        }
    }

    @DiscordCommand(key = "delete", params = "<amount>", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public class DeleteCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<TextChannel> reply = ref.getReplyChannel().cast(TextChannel.class);
            if(!MessageUtil.canParseInt(args[0])){
                return messageService.err(reply, messageService.get(ref.context(), "command.incorrect-number"));
            }

            int number = Strings.parseInt(args[0]);
            if(number >= settings.maxClearedCount){
                return messageService.err(reply, messageService.format(ref.context(), "common.limit-number", settings.maxClearedCount));
            }

            Mono<List<Message>> history = reply.flatMapMany(channel -> channel.getMessagesBefore(ref.getMessage().getId()))
                    .limitRequest(number)
                    .collectSortedList(Comparator.comparing(Message::getId));

            return Mono.zip(reply, author.getGuild(), history).flatMap(TupleUtils.function((channel, guild, messages) -> Mono.fromRunnable(() ->
                    discordService.eventListener().publish(new MessageClearEvent(guild, messages, author, channel, number))
            )));
        }
    }

    @DiscordCommand(key = "warn", params = "<@user> [reason...]", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public class WarnCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();
            if(targetId == null || !discordService.existsMemberById(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target))
                            .filterWhen(local -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author)).map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin")).then(Mono.empty()))
                            .flatMap(local -> {
                                String reason = args.length > 1 ? args[1].trim() : null;

                                if(Objects.equals(author, target)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.warn.self-user"));
                                }

                                if(!MessageUtil.isEmpty(reason) && reason.length() >= 512){
                                    return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 512));
                                }

                                Mono<Void> warnings = Mono.defer(() -> adminService.warnings(local.guildId(), local.userId()).count()).flatMap(count -> {
                                    Mono<Void> message = messageService.text(channel, messageService.format(ref.context(), "command.admin.warn", target.getUsername(), count));

                                    if(count >= settings.maxWarnings){
                                        return message.then(author.getGuild().flatMap(guild -> guild.ban(target.getId(), b -> b.setDeleteMessageDays(0))));
                                    }
                                    return message;
                                });

                                return adminService.warn(ref.localMember(), local, reason).then(warnings);
                            })
                    );
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.admin.warnings.description")
    public class WarningsCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = author.getGuildId();
            if(targetId == null || !discordService.existsMemberById(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            final DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                    .withLocale(ref.context().get(KEY_LOCALE))
                    .withZone(ref.context().get(KEY_TIMEZONE));

            return discordService.gateway().getMemberById(guildId, targetId)
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target))
                            .flatMap(local -> {
                                Flux<AdminAction> warnings = adminService.warnings(local.guildId(), local.userId()).limitRequest(21);

                                Mono<Void> warningMessage = Mono.defer(() -> messageService.info(channel, embed ->
                                        warnings.index().subscribe(TupleUtils.consumer((index, warn) -> embed.setColor(settings.normalColor)
                                                .setTitle(messageService.format(ref.context(), "command.admin.warnings.title", local.effectiveName()))
                                                .addField(String.format("%2s. %s", index + 1, formatter.print(new DateTime(warn.timestamp()))), String.format("%s%n%s",
                                                messageService.format(ref.context(), "common.admin", warn.admin().effectiveName()),
                                                messageService.format(ref.context(), "common.reason", warn.reason().orElse(messageService.get(ref.context(), "common.not-defined")))
                                                ), true)))
                                ));

                                return warnings.hasElements().flatMap(bool -> !bool ? messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty")) : warningMessage);
                            }));
        }
    }

    @DiscordCommand(key = "unwarn", params = "<@user> [number]", description = "command.admin.unwarn.description")
    public class UnwarnCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();
            if(targetId == null || !discordService.existsMemberById(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
            }

            return adminService.warnings(guildId, targetId).count().flatMap(count -> {
                int warn = args.length > 1 ? Strings.parseInt(args[1]) : 1;
                if(count == 0){
                    return messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty"));
                }

                if(warn > count){
                    return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
                }

                return discordService.gateway().getMemberById(guildId, targetId)
                        .map(entityRetriever::getMember)
                        .flatMap(local -> messageService.text(channel, messageService.format(ref.context(), "command.admin.unwarn", local.effectiveName(), warn))
                                .then(adminService.unwarn(local.guildId(), local.userId(), warn - 1)));
            });
        }
    }

    @DiscordCommand(key = "unmute", params = "<@user>", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public class UnmuteCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.localMember().guildId();

            if(entityRetriever.muteRoleId(guildId).isEmpty()){
                return messageService.err(channel, messageService.get(ref.context(), "command.disabled.mute"));
            }

            if(targetId == null || !discordService.existsMemberById(guildId, targetId)){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name"));
            }

            Mono<Member> target = discordService.gateway().getMemberById(guildId, targetId);

            return target.flatMap(member -> Mono.just(entityRetriever.getMember(member))
                    .filterWhen(local -> adminService.isMuted(local.guildId(), local.userId()))
                    .flatMap(local -> ref.event().getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(new MemberUnmuteEvent(guild, local)))).thenReturn(member))
                    .switchIfEmpty(messageService.err(channel, messageService.format(ref.context(), "audit.member.unmute.is-not-muted", member.getUsername())).then(Mono.empty())))
                    .then();
        }
    }
}
