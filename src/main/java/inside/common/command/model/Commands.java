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
import inside.event.audit.*;
import inside.util.*;
import org.joda.time.*;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static inside.event.audit.Attribute.COUNT;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;

// TODO(Skat): delete synthetics?
@Collector
public class Commands{
    private static final Logger log = Loggers.getLogger(Commands.class);

    @Autowired
    private MessageService messageService;

    @Lazy
    @Autowired
    private AdminService adminService;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private CommandHandler handler;

    @Autowired
    private Settings settings;

    @Autowired
    private AuditService auditService;

    public abstract class ModeratorCommand extends Command{
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

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .flatMap(user -> messageService.info(channel, embed -> embed.setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(ref.context(), "command.avatar.text", user.getUsername()))));
        }
    }

    @DiscordCommand(key = "1337", params = "<text...>", description = "command.1337.description")
    public class LeetCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(MessageUtil.leeted(args[0]), Message.MAX_CONTENT_LENGTH));
        }
    }

    @DiscordCommand(key = "tr", params = "<text...>", description = "command.translit.description")
    public class TranslitCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(MessageUtil.translit(args[0]), Message.MAX_CONTENT_LENGTH));
        }
    }

    @DiscordCommand(key = "prefix", params = "[prefix]", description = "command.config.prefix.description")
    public class PrefixCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();

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

    @DiscordCommand(key = "timezone", params = "[timezone]", description = "command.config.timezone.description")
    public class TimezoneCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();

            return Mono.just(entityRetriever.getGuildById(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isOwner(member).map(bool -> bool && args.length > 0))
                    .flatMap(guildConfig -> Mono.defer(() -> {
                        DateTimeZone timeZone = MessageUtil.find(args[0]);
                        if(timeZone == null){
                            int min = 0;
                            String suggest = null;

                            for(String z : DateTimeZone.getAvailableIDs()){
                                int dst = Strings.levenshtein(z, args[0]);
                                if(dst < 3 && (suggest == null || dst < min)){
                                    min = dst;
                                    suggest = z;
                                }
                            }

                            if(suggest != null){
                                return messageService.err(channel, messageService.format(ref.context(), "command.config.unknown-timezone.suggest", suggest));
                            }
                            return messageService.err(channel, messageService.format(ref.context(), "command.config.unknown-timezone"));
                        }

                        guildConfig.timeZone(timeZone.toTimeZone());
                        entityRetriever.save(guildConfig);
                        return Mono.deferContextual(ctx -> messageService.text(channel, messageService.format(ctx, "command.config.timezone-updated", ctx.<Locale>get(KEY_TIMEZONE))))
                                .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(args.length == 0 ?
                                   messageService.text(channel, messageService.format(ref.context(), "command.config.timezone", ref.context().<Locale>get(KEY_TIMEZONE))).then(Mono.empty()) :
                                   messageService.err(channel, messageService.get(ref.context(), "command.owner-only")).then(Mono.empty()))
                    .then(Mono.empty());
        }
    }

    @DiscordCommand(key = "locale", params = "[locale]", description = "command.config.locale.description")
    public class LocaleCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member member = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();

            return Mono.just(entityRetriever.getGuildById(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isOwner(member).map(bool -> bool && args.length > 0))
                    .flatMap(guildConfig -> Mono.defer(() -> {
                        Locale locale = LocaleUtil.get(args[0]);
                        if(locale == null){
                            String all = LocaleUtil.locales.values().stream()
                                    .map(Locale::toString)
                                    .collect(Collectors.joining(", "));

                            return messageService.text(channel, messageService.format(ref.context(), "command.config.unknown-locale", all));
                        }

                        guildConfig.locale(locale);
                        entityRetriever.save(guildConfig);
                        return Mono.deferContextual(ctx -> messageService.text(channel, messageService.format(ctx, "command.config.locale-updated", ctx.<Locale>get(KEY_LOCALE))))
                                .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(args.length == 0 ?
                            messageService.text(channel, messageService.format(ref.context(), "command.config.locale", ref.context().<Locale>get(KEY_LOCALE))).then(Mono.empty()) :
                            messageService.err(channel, messageService.get(ref.context(), "command.owner-only")).then(Mono.empty()))
                    .then(Mono.empty());
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

            DateTime delay = MessageUtil.parseTime(args[1]);
            if(delay == null){
                return messageService.err(channel, messageService.get(ref.context(), "message.error.invalid-time"));
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .transform(target -> target.filterWhen(member -> adminService.isMuted(member).map(bool -> !bool))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.already-muted")).then(Mono.never()))
                            .filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author)).map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin")).then(Mono.empty()))
                            .flatMap(member -> Mono.defer(() -> {
                                String reason = args.length > 2 ? args[2].trim() : null;

                                if(Objects.equals(author, member)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.mute.self-user"));
                                }

                                if(reason != null && !reason.isBlank() && reason.length() >= 512){
                                    return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 512));
                                }

                                return adminService.mute(author, member, delay, reason);
                            }))
                            .then());
        }
    }

    // bulk delete test
    // @DiscordCommand(key = "test", params = "<count>", description = "")
    // public class TestCommand extends ModeratorCommand{
    //     @Override
    //     public Mono<Void> execute(CommandReference ref, String[] args){
    //         Mono<TextChannel> reply = ref.getReplyChannel()
    //                 .ofType(TextChannel.class);
    //
    //         int number = Strings.parseInt(args[0]);
    //         if(number >= settings.maxClearedCount){
    //             return messageService.err(reply, messageService.format(ref.context(), "common.limit-number", settings.maxClearedCount));
    //         }
    //
    //         return reply.flatMapMany(textChannel -> textChannel.getMessagesBefore(ref.getMessage().getId())
    //                 .take(number)
    //                 .map(Message::getId)
    //                 .transform(textChannel::bulkDelete))
    //                 .then();
    //     }
    // }

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

            StringBuffer result = new StringBuffer();
            Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                    .withLocale(ref.context().get(KEY_LOCALE))
                    .withZone(ref.context().get(KEY_TIMEZONE));

            StringInputStream input = new StringInputStream();
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

            AuditActionBuilder builder = auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withAttribute(COUNT, number);

            Mono<Void> history = reply.flatMapMany(channel -> channel.getMessagesBefore(ref.getMessage().getId()))
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .flatMap(member -> Mono.fromRunnable(() -> {
                                messageService.putMessage(message.getId());
                                appendInfo.accept(message, member);
                            }))
                            .and(message.delete()))
                    .then();

            Mono<Void> log =  ref.getReplyChannel()
                    .ofType(GuildChannel.class)
                    .flatMap(channel -> builder.withChannel(channel)
                            .withAttachment(MESSAGE_TXT, input.writeString(result.toString()))
                            .save());

            return history.then(log);
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

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .transform(target -> target.filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                            .map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.user-is-admin")).then(Mono.empty()))
                            .flatMap(member -> {
                                String reason = args.length > 1 ? args[1].trim() : null;

                                if(Objects.equals(author, member)){
                                    return messageService.err(channel, messageService.get(ref.context(), "command.admin.warn.self-user"));
                                }

                                if(!MessageUtil.isEmpty(reason) && reason.length() >= 512){
                                    return messageService.err(channel, messageService.format(ref.context(), "common.string-limit", 512));
                                }

                                Mono<Void> warnings = Mono.defer(() -> adminService.warnings(member).count()).flatMap(count -> {
                                    Mono<Void> message = messageService.text(channel, messageService.format(ref.context(), "command.admin.warn", member.getUsername(), count));

                                    if(count >= settings.maxWarnings){
                                        return message.then(author.getGuild().flatMap(guild -> guild.ban(member.getId(), spec -> spec.setDeleteMessageDays(0))));
                                    }
                                    return message;
                                });

                                return adminService.warn(author, member, reason).then(warnings);
                            }));
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.admin.warnings.description")
    public class WarningsCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();

            DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                    .withLocale(ref.context().get(KEY_LOCALE))
                    .withZone(ref.context().get(KEY_TIMEZONE));

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .flatMap(target -> {
                        Flux<AdminAction> warnings = adminService.warnings(target.getGuildId(), target.getId()).limitRequest(21);

                        Mono<Void> warningMessage = Mono.defer(() -> messageService.info(channel, embed ->
                                warnings.index().subscribe(TupleUtils.consumer((index, warn) ->
                                        embed.setTitle(messageService.format(ref.context(), "command.admin.warnings.title", target.getDisplayName()))
                                        .addField(String.format("%2s. %s", index + 1, formatter.print(warn.timestamp())), String.format("%s%n%s",
                                        messageService.format(ref.context(), "common.admin", warn.admin().effectiveName()),
                                        messageService.format(ref.context(), "common.reason", warn.reason().orElse(messageService.get(ref.context(), "common.not-defined")))
                                        ), true)))
                                ));

                        return warnings.hasElements().flatMap(bool -> !bool ? messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty")) : warningMessage);
                    });
        }
    }

    @DiscordCommand(key = "unwarn", params = "<@user> [number]", description = "command.admin.unwarn.description")
    public class UnwarnCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();

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

                return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                        .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                        .flatMap(target -> messageService.text(channel, messageService.format(ref.context(), "command.admin.unwarn", target.getUsername(), warn))
                                .then(adminService.unwarn(target, warn - 1)));
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
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();

            if(entityRetriever.muteRoleId(guildId).isEmpty()){
                return messageService.err(channel, messageService.get(ref.context(), "command.disabled.mute"));
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .transform(member -> member.filterWhen(adminService::isMuted)
                            .flatMap(target -> adminService.unmute(target).thenReturn(target))
                            .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "audit.member.unmute.is-not-muted")).then(Mono.empty())))
                    .then();
        }
    }
}
