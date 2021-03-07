package inside.command;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.presence.Presence;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.command.model.*;
import inside.data.entity.AdminAction;
import inside.data.service.AdminService;
import inside.event.audit.*;
import inside.util.*;
import org.joda.time.*;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static inside.data.service.MessageService.ok;
import static inside.event.audit.Attribute.COUNT;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;

public class Commands{

    private Commands(){}

    public static abstract class ModeratorCommand extends Command{
        @Lazy
        @Autowired
        protected AdminService adminService;

        @Override
        public Mono<Boolean> apply(CommandRequest req){
            return adminService.isAdmin(req.getAuthorAsMember());
        }
    }

    public static abstract class TestCommand extends Command{
        @Override
        public Mono<Boolean> apply(CommandRequest req){
            return req.getClient().getApplicationInfo()
                    .map(ApplicationInfo::getOwnerId)
                    .map(owner -> owner.equals(req.getAuthorAsMember().getId()));
        }
    }

    @DiscordCommand(key = "help", description = "command.help.description")
    public static class HelpCommand extends Command{
        @Autowired
        private CommandHandler handler;

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
                    builder.append(messageService.get(ref.context(), command.paramText));
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

    @DiscordCommand(key = "avatar", params = "command.avatar.params", description = "command.avatar.description")
    public static class AvatarCommand extends Command{
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

    @DiscordCommand(key = "status", params = "command.status.params", description = "command.status.description")
    public static class StatusCommand extends TestCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return switch(args[0].toLowerCase()){
                case "online" -> ref.getClient().updatePresence(Presence.online());
                case "dnd" -> ref.getClient().updatePresence(Presence.doNotDisturb());
                case "idle" -> ref.getClient().updatePresence(Presence.idle());
                case "invisible" -> ref.getClient().updatePresence(Presence.invisible());
                default -> messageService.err(ref.getReplyChannel(), messageService.get(ref.context(), "command.status.unknown-presence"));
            };
        }
    }

    @DiscordCommand(key = "r", params = "command.text-layout.params", description = "command.text-layout.description")
    public static class TextLayoutCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            boolean lat = args[0].equalsIgnoreCase("lat");
            return messageService.text(ref.getReplyChannel(), lat ? MessageUtil.text2rus(args[1]) : MessageUtil.text2lat(args[1]));
        }
    }

    @DiscordCommand(key = "1337", params = "command.1337.params", description = "command.1337.description")
    public static class LeetCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(MessageUtil.leeted(args[0]), Message.MAX_CONTENT_LENGTH));
        }
    }

    @DiscordCommand(key = "tr", params = "command.translit.params", description = "command.translit.description")
    public static class TranslitCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(MessageUtil.translit(args[0]), Message.MAX_CONTENT_LENGTH));
        }
    }

    @DiscordCommand(key = "prefix", params = "command.config.prefix.params", description = "command.config.prefix.description")
    public static class PrefixCommand extends Command{
        @Autowired
        private AdminService adminService;

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

    @DiscordCommand(key = "timezone", params = "command.config.timezone.params", description = "command.config.timezone.description")
    public static class TimezoneCommand extends Command{
        @Autowired
        private AdminService adminService;

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

    @DiscordCommand(key = "locale", params = "command.config.locale.params", description = "command.config.locale.description")
    public static class LocaleCommand extends Command{
        @Autowired
        private AdminService adminService;

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

    @DiscordCommand(key = "mute", params = "command.admin.mute.params", description = "command.admin.mute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public static class MuteCommand extends ModeratorCommand{
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
                            .filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                                    .map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
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
                            .then())
                    .and(ref.getMessage().addReaction(ok));
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

    @DiscordCommand(key = "delete", params = "command.admin.delete.params", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public static class DeleteCommand extends ModeratorCommand{
        @Autowired
        private Settings settings;

        @Autowired
        private AuditService auditService;

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

            return history.then(log).and(ref.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = "warn", params = "command.admin.warn.params", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public static class WarnCommand extends ModeratorCommand{
        @Autowired
        private Settings settings;

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

    @DiscordCommand(key = "warnings", params = "command.admin.warnings.params", description = "command.admin.warnings.description")
    public static class WarningsCommand extends ModeratorCommand{
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

    @DiscordCommand(key = "unwarn", params = "command.admin.unwarn.params", description = "command.admin.unwarn.description")
    public static class UnwarnCommand extends ModeratorCommand{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Member author = ref.getAuthorAsMember();
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = MessageUtil.parseUserId(args[0]);
            Snowflake guildId = ref.getAuthorAsMember().getGuildId();

            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.never()))
                    .filterWhen(target -> adminService.isOwner(author).map(owner -> !target.equals(author) || owner))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.admin.unwarn.permission-denied")).then(Mono.empty()))
                    .flatMap(target -> adminService.warnings(target).count().flatMap(count -> {
                        int warn = args.length > 1 ? Strings.parseInt(args[1]) : 1;
                        if(count == 0){
                            return messageService.text(channel, messageService.get(ref.context(), "command.admin.warnings.empty"));
                        }

                        if(warn > count){
                            return messageService.err(channel, messageService.get(ref.context(), "command.incorrect-number"));
                        }

                        return messageService.text(channel, messageService.format(ref.context(), "command.admin.unwarn", target.getUsername(), warn))
                                .and(adminService.unwarn(target, warn - 1));
                    }));
        }
    }

    @DiscordCommand(key = "unmute", params = "command.admin.unmute.params", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public static class UnmuteCommand extends ModeratorCommand{
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
