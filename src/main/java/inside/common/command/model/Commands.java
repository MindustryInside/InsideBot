package inside.common.command.model;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
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
import org.joda.time.DateTime;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.*;

import java.util.*;
import java.util.function.*;
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
                        if(args.length == 0){
                            return messageService.text(channel, messageService.format(ref.context(), "command.config.locale", ref.context().<Locale>getOrEmpty(KEY_LOCALE)));
                        }else{
                            if(!args[0].isBlank()){
                                Locale locale = LocaleUtil.get(args[0]);
                                if(locale == null){
                                    String all = LocaleUtil.locales.values().stream()
                                            .map(Locale::toString)
                                            .collect(Collectors.joining(", "));

                                    return messageService.text(channel, messageService.format(ref.context(), "command.config.unknown", all));
                                }

                                guildConfig.locale(locale);
                                entityRetriever.save(guildConfig);
                                return Mono.deferContextual(ctx -> messageService.text(channel, messageService.format(ctx, "command.config.locale-updated", ctx.<Locale>get(KEY_LOCALE))))
                                        .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale));
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

            DateTime delay = MessageUtil.parseTime(args[1]);
            if(delay == null){
                return messageService.err(channel, messageService.get(ref.context(), "message.error.invalid-time"));
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
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

                                return adminService.mute(author, target, delay, reason);
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

            StringBuffer result = new StringBuffer();
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
                result.append(" > ");
                if(!MessageUtil.isEmpty(message)){
                    result.append(MessageUtil.effectiveContent(message));
                }

                for(int i = 0; i < message.getEmbeds().size(); i++){
                    Embed embed = message.getEmbeds().get(i);
                    result.append("\n[embed-").append(i + 1).append("]");
                    embed.getDescription().ifPresent(s -> result.append("\n").append(s));
                }
                result.append("\n");
            };

            AuditActionBuilder builder = auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withAttribute(COUNT, number);

            Mono<Void> history = reply.flatMapMany(channel -> channel.getMessagesBefore(ref.getMessage().getId()))
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .flatMap(message -> message.getAuthorAsMember().flatMap(member -> Mono.fromRunnable(() -> {
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

                                return adminService.warn(author, target, reason).then(warnings);
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

            DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                    .withLocale(ref.context().get(KEY_LOCALE))
                    .withZone(ref.context().get(KEY_TIMEZONE));

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, messageService.get(ref.context(), "command.incorrect-name")).then(Mono.empty()))
                    .flatMap(target -> Mono.just(entityRetriever.getMember(target))
                            .flatMap(local -> {
                                Flux<AdminAction> warnings = adminService.warnings(local.guildId(), local.userId()).limitRequest(21);

                                Mono<Void> warningMessage = Mono.defer(() -> messageService.info(channel, embed ->
                                        warnings.index().subscribe(TupleUtils.consumer((index, warn) ->
                                                embed.setTitle(messageService.format(ref.context(), "command.admin.warnings.title", local.effectiveName()))
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
                    .flatMap(member -> Mono.just(entityRetriever.getMember(member))
                            .filterWhen(local -> adminService.isMuted(local.guildId(), local.userId()))
                            .flatMap(local -> adminService.unmute(member).thenReturn(member))
                            .switchIfEmpty(messageService.err(channel, messageService.format(ref.context(), "audit.member.unmute.is-not-muted", member.getUsername())).then(Mono.empty())))
                    .then();
        }
    }
}
