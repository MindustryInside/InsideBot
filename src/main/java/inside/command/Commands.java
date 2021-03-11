package inside.command;

import arc.struct.StringMap;
import arc.util.*;
import com.udojava.evalex.*;
import discord4j.common.ReactorResources;
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
import io.netty.handler.codec.http.HttpResponseStatus;
import org.joda.time.*;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.*;
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
                builder.append(command.text());
                builder.append("**");
                if(command.params().length > 0){
                    builder.append(" *");
                    builder.append(messageService.get(ref.context(), command.paramText()));
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(messageService.get(ref.context(), command.description()));
                builder.append("\n");
            });
            builder.append(messageService.get(ref.context(), "command.help.disclaimer.user"));

            return messageService.info(ref.getReplyChannel(),"command.help", builder.toString());
        }
    }

    @DiscordCommand(key = "ping", description = "command.ping.description")
    public static class PingCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            long start = System.currentTimeMillis();
            return ref.getReplyChannel()
                    .flatMap(channel -> channel.createMessage(
                            messageService.get(ref.context(), "command.ping.testing")))
                    .flatMap(message -> message.edit(spec -> spec.setContent(
                            messageService.format(ref.context(), "command.ping.completed", Time.timeSinceMillis(start)))))
                    .then();
        }
    }

    @DiscordCommand(key = "avatar", params = "command.avatar.params", description = "command.avatar.description")
    public static class AvatarCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<MessageChannel> channel = ref.getReplyChannel();
            Snowflake targetId = args.length > 0 ? MessageUtil.parseUserId(args[0]) : ref.getAuthorAsMember().getId();

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .flatMap(user -> messageService.info(channel, embed -> embed.setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(ref.context(), "command.avatar.text", user.getUsername()))));
        }
    }

    @DiscordCommand(key = "math", params = "command.math.params", description = "command.math.description")
    public static class MathCommand extends Command{
        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<BigDecimal> result = Mono.fromCallable(() -> {
                Expression exp = new Expression(args[0]).setPrecision(10);
                exp.addOperator(shiftRightOperator);
                exp.addOperator(shiftLeftOperator);
                return exp.eval();
            });

            return result.onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException,
                    t -> messageService.error(ref.getReplyChannel(), "command.math.error.title", t.getMessage()).then(Mono.empty()))
                    .flatMap(decimal -> messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
        }

        private static final LazyOperator shiftRightOperator = new AbstractOperator(">>", 30, true){
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2){
                return v1.movePointRight(v2.toBigInteger().intValue());
            }
        };

        private static final LazyOperator shiftLeftOperator = new AbstractOperator("<<", 30, true){
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2){
                return v1.movePointLeft(v2.toBigInteger().intValue());
            }
        };
    }

    @DiscordCommand(key = "read", params = "command.read.params", description = "command.read.description")
    public static class ReadCommand extends Command{
        private final HttpClient httpClient = ReactorResources.DEFAULT_HTTP_CLIENT.get();

        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            Mono<String> attachmentUrl = Mono.justOrEmpty(ref.getMessage().getAttachments().stream().findFirst())
                    .switchIfEmpty(messageService.err(ref.getReplyChannel(), "command.read.empty-attachments").then(Mono.never()))
                    .filter(attachment -> attachment.getSize() < 3145728) // 3mb
                    .switchIfEmpty(messageService.err(ref.getReplyChannel(), "command.read.under-limit").then(Mono.never()))
                    .filter(attachment -> attachment.getWidth().isEmpty())
                    .switchIfEmpty(messageService.err(ref.getReplyChannel(), "command.read.image").then(Mono.empty()))
                    .map(Attachment::getUrl);

            return Mono.justOrEmpty(args.length > 0 ? args[0] : null)
                    .switchIfEmpty(attachmentUrl)
                    .flatMap(url -> httpClient.get()
                            .uri(url)
                            .responseSingle((res, mono) -> res.status().equals(HttpResponseStatus.OK) ? mono.filter(buf -> buf.capacity() < 3145728)
                                    .switchIfEmpty(messageService.err(ref.getReplyChannel(), "command.read.under-limit").then(Mono.never()))
                                    .map(buf -> buf.readCharSequence(buf.readableBytes(), Strings.utf8).toString()) : Mono.empty()))
                    .onErrorResume(t -> true, t -> Mono.empty())
                    .switchIfEmpty(messageService.err(ref.getReplyChannel(), "command.read.error").then(Mono.empty()))
                    .map(content -> MessageUtil.substringTo(content, Message.MAX_CONTENT_LENGTH))
                    .flatMap(content -> messageService.text(ref.getReplyChannel(), content));
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
                default -> messageService.err(ref.getReplyChannel(), "command.status.unknown-presence");
            };
        }
    }

    @DiscordCommand(key = "r", params = "command.text-layout.params", description = "command.text-layout.description")
    public static class TextLayoutCommand extends Command{
        private static final String[] latPattern;
        private static final String[] rusPattern;

        static{
            String lat = "Q-W-E-R-T-Y-U-I-O-P-A-S-D-F-G-H-J-K-L-Z-X-C-V-B-N-M";
            String rus = "Й-Ц-У-К-Е-Н-Г-Ш-Щ-З-Ф-Ы-В-А-П-Р-О-Л-Д-Я-Ч-С-М-И-Т-Ь";
            latPattern = (lat + "-" + lat.toLowerCase() + "-\\^-:-\\$-@-&-~-`-\\{-\\[-\\}-\\]-\"-'-<->-;-\\?-\\/-\\.-,-#").split("-");
            rusPattern = (rus + "-" + rus.toLowerCase() + "-:-Ж-;-\"-\\?-Ё-ё-Х-х-Ъ-ъ-Э-э-Б-Ю-ж-,-\\.-ю-б-№").split("-");
        }

        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            boolean lat = args[0].equalsIgnoreCase("lat");
            return messageService.text(ref.getReplyChannel(), lat ? text2rus(args[1]) : text2lat(args[1]));
        }

        public String text2rus(String text){
            for(int i = 0; i < latPattern.length; i++){
                text = text.replaceAll("(?u)" + latPattern[i], rusPattern[i]);
            }
            return text;
        }

        public String text2lat(String text){
            for(int i = 0; i < rusPattern.length; i++){
                text = text.replaceAll("(?u)" + rusPattern[i], latPattern[i]);
            }
            return text;
        }
    }

    @DiscordCommand(key = "1337", params = "command.1337.params", description = "command.1337.description")
    public static class LeetCommand extends Command{
        public static final StringMap rusLeetSpeak;
        public static final StringMap latLeetSpeak;

        static{
            rusLeetSpeak = StringMap.of(
                    "а", "4", "б", "6", "в", "8", "г", "g",
                    "д", "d", "е", "3", "ё", "3", "ж", "zh",
                    "з", "e", "и", "i", "й", "\\`i", "к", "k",
                    "л", "l", "м", "m", "н", "n", "о", "0",
                    "п", "p", "р", "r", "с", "c", "т", "7",
                    "у", "y", "ф", "f", "х", "x", "ц", "u,",
                    "ч", "ch", "ш", "w", "щ", "w,", "ъ", "\\`ь",
                    "ы", "ьi", "ь", "ь", "э", "э", "ю", "10",
                    "я", "9"
            );

            latLeetSpeak = StringMap.of(
                    "a", "4", "b", "8", "c", "c", "d", "d",
                    "e", "3", "f", "ph", "g", "9", "h", "h",
                    "i", "1", "j", "g", "k", "k", "l", "l",
                    "m", "m", "n", "n", "o", "0", "p", "p",
                    "q", "q", "r", "r", "s", "5", "t", "7",
                    "u", "u", "v", "v", "w", "w", "x", "x",
                    "y", "y", "z", "2"
            );
        }

        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            boolean lat = args[0].equalsIgnoreCase("lat");
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(leeted(args[1], lat), Message.MAX_CONTENT_LENGTH));
        }

        public String leeted(String text, boolean lat){
            StringMap map = lat ? latLeetSpeak : rusLeetSpeak;
            UnaryOperator<String> get = s -> {
                String result = map.get(s.toLowerCase());
                if(result == null){
                    result = map.findKey(s.toLowerCase(), false);
                }
                return result != null ? s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result : "";
            };

            int len = text.length();
            if(len == 1){
                return get.apply(text);
            }

            StringBuilder result = new StringBuilder();
            for(int i = 0; i < len; ){
                String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
                String leeted = get.apply(c);
                if(MessageUtil.isEmpty(leeted)){
                    leeted = get.apply(c.charAt(0) + "");
                    result.append(MessageUtil.isEmpty(leeted) ? c.charAt(0) : leeted);
                    i++;
                }else{
                    result.append(leeted);
                    i += 2;
                }
            }
            return result.toString();
        }
    }

    @DiscordCommand(key = "tr", params = "command.translit.params", description = "command.translit.description")
    public static class TranslitCommand extends Command{
        public static final StringMap translit;

        static{
            translit = StringMap.of(
                    "a", "а", "b", "б", "v", "в", "g", "г",
                    "d", "д", "e", "е", "yo", "ё", "zh", "ж",
                    "z", "з", "i", "и", "j", "й", "k", "к",
                    "l", "л", "m", "м", "n", "н", "o", "о",
                    "p", "п", "r", "р", "s", "с", "t", "т",
                    "u", "у", "f", "ф", "h", "х", "ts", "ц",
                    "ch", "ч", "sh", "ш", "\\`", "ъ", "y", "у",
                    "'", "ь", "yu", "ю", "ya", "я", "x", "кс",
                    "v", "в", "q", "к", "iy", "ий"
            );
        }

        @Override
        public Mono<Void> execute(CommandReference ref, String[] args){
            return messageService.text(ref.getReplyChannel(), MessageUtil.substringTo(translit(args[0]), Message.MAX_CONTENT_LENGTH));
        }

        public String translit(String text){
            UnaryOperator<String> get = s -> {
                String result = translit.get(s.toLowerCase());
                if(result == null){
                    result = translit.findKey(s.toLowerCase(), false);
                }
                return result != null ? s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result : "";
            };

            int len = text.length();
            if(len == 1){
                return get.apply(text);
            }

            StringBuilder result = new StringBuilder();
            for(int i = 0; i < len; ){
                String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
                String translited = get.apply(c);
                if(MessageUtil.isEmpty(translited)){
                    translited = get.apply(c.charAt(0) + "");
                    result.append(MessageUtil.isEmpty(translited) ? c.charAt(0) : translited);
                    i++;
                }else{
                    result.append(translited);
                    i += 2;
                }
            }
            return result.toString();
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
                    .switchIfEmpty(messageService.err(channel, "command.owner-only").then(Mono.empty()))
                    .flatMap(guildConfig -> {
                        if(args.length == 0){
                            return messageService.text(channel, "command.config.prefix", guildConfig.prefix());
                        }else{
                            if(!args[0].isBlank()){
                                guildConfig.prefix(args[0]);
                                entityRetriever.save(guildConfig);
                                return messageService.text(channel, "command.config.prefix-updated", guildConfig.prefix());
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
                            String suggest = MessageUtil.findClosest(DateTimeZone.getAvailableIDs(), Function.identity(), args[0]);

                            if(suggest != null){
                                return messageService.err(channel, "command.config.unknown-timezone.suggest", suggest);
                            }
                            return messageService.err(channel, "command.config.unknown-timezone");
                        }

                        guildConfig.timeZone(timeZone.toTimeZone());
                        entityRetriever.save(guildConfig);
                        return Mono.deferContextual(ctx -> messageService.text(channel, "command.config.timezone-updated", ctx.<Locale>get(KEY_TIMEZONE)))
                                .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(args.length == 0 ?
                                   messageService.text(channel, "command.config.timezone", ref.context().<Locale>get(KEY_TIMEZONE)).then(Mono.empty()) :
                                   messageService.err(channel, "command.owner-only").then(Mono.empty()))
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

                            return messageService.text(channel, "command.config.unknown-locale", all);
                        }

                        guildConfig.locale(locale);
                        entityRetriever.save(guildConfig);
                        return Mono.deferContextual(ctx -> messageService.text(channel, "command.config.locale-updated", ctx.<Locale>get(KEY_LOCALE)))
                                .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(args.length == 0 ?
                            messageService.text(channel, "command.config.locale", ref.context().<Locale>get(KEY_LOCALE)).then(Mono.empty()) :
                            messageService.err(channel, "command.owner-only").then(Mono.empty()))
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
                return messageService.err(channel, "command.disabled.mute");
            }

            DateTime delay = MessageUtil.parseTime(args[1]);
            if(delay == null){
                return messageService.err(channel, "message.error.invalid-time");
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .transform(target -> target.filterWhen(member -> adminService.isMuted(member).map(bool -> !bool))
                            .switchIfEmpty(messageService.err(channel, "command.admin.mute.already-muted").then(Mono.never()))
                            .filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                                    .map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, "command.admin.user-is-admin").then(Mono.empty()))
                            .flatMap(member -> Mono.defer(() -> {
                                String reason = args.length > 2 ? args[2].trim() : null;

                                if(Objects.equals(author, member)){
                                    return messageService.err(channel, "command.admin.mute.self-user");
                                }

                                if(reason != null && !reason.isBlank() && reason.length() >= 512){
                                    return messageService.err(channel, "common.string-limit", 512);
                                }

                                return adminService.mute(author, member, delay, reason);
                            }))
                            .then())
                    .and(ref.getMessage().addReaction(ok));
        }
    }

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
                return messageService.err(reply, "command.incorrect-number");
            }

            int number = Strings.parseInt(args[0]);
            if(number >= settings.maxClearedCount){
                return messageService.err(reply, "common.limit-number", settings.maxClearedCount);
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

            Mono<Void> history = reply.flatMapMany(channel -> channel.getMessagesBefore(ref.getMessage().getId())
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .doOnNext(member -> {
                                appendInfo.accept(message, member);
                                messageService.deleteById(message.getId());
                            })
                            .thenReturn(message))
                    .transform(messages -> number != 1 ? channel.bulkDeleteMessages(messages).then() : messages.next().flatMap(Message::delete).then()))
                    .then();

            Mono<Void> log =  reply.flatMap(channel -> auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withChannel(channel)
                    .withAttribute(COUNT, number)
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
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .transform(target -> target.filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                            .map(TupleUtils.function((admin, owner) -> !(admin && !owner))))
                            .switchIfEmpty(messageService.err(channel, "command.admin.user-is-admin").then(Mono.empty()))
                            .flatMap(member -> {
                                String reason = args.length > 1 ? args[1].trim() : null;

                                if(Objects.equals(author, member)){
                                    return messageService.err(channel, "command.admin.warn.self-user");
                                }

                                if(!MessageUtil.isEmpty(reason) && reason.length() >= 512){
                                    return messageService.err(channel, "common.string-limit", 512);
                                }

                                Mono<Void> warnings = Mono.defer(() -> adminService.warnings(member).count()).flatMap(count -> {
                                    Mono<Void> message = messageService.text(channel, "command.admin.warn", member.getUsername(), count);

                                    if(count >= settings.maxWarnings){
                                        return message.then(author.getGuild().flatMap(guild ->
                                                guild.ban(member.getId(), spec -> spec.setDeleteMessageDays(0))));
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
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .flatMap(target -> {
                        Flux<AdminAction> warnings = adminService.warnings(target.getGuildId(), target.getId()).limitRequest(21);

                        Mono<Void> warningMessage = Mono.defer(() -> messageService.info(channel, embed ->
                                warnings.index().subscribe(TupleUtils.consumer((index, warn) ->
                                        embed.setTitle(messageService.format(ref.context(), "command.admin.warnings.title", target.getDisplayName()))
                                        .addField(String.format("%2s. %s", index + 1, formatter.print(warn.timestamp())), String.format("%s%n%s",
                                        messageService.format(ref.context(), "common.admin", warn.admin().effectiveName()),
                                        messageService.format(ref.context(), "common.reason", warn.reason()
                                                .orElse(messageService.get(ref.context(), "common.not-defined")))
                                        ), true)))
                                ));

                        return warnings.hasElements().flatMap(bool -> !bool ? messageService.text(channel, "command.admin.warnings.empty") : warningMessage);
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
                return messageService.err(channel, "command.incorrect-number");
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> ref.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.never()))
                    .filterWhen(target -> adminService.isOwner(author).map(owner -> !target.equals(author) || owner))
                    .switchIfEmpty(messageService.err(channel, "command.admin.unwarn.permission-denied").then(Mono.empty()))
                    .flatMap(target -> adminService.warnings(target).count().flatMap(count -> {
                        int warn = args.length > 1 ? Strings.parseInt(args[1]) : 1;
                        if(count == 0){
                            return messageService.text(channel, "command.admin.warnings.empty");
                        }

                        if(warn > count){
                            return messageService.err(channel, "command.incorrect-number");
                        }

                        return messageService.text(channel, "command.admin.unwarn", target.getUsername(), warn)
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
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .transform(member -> member.filterWhen(adminService::isMuted)
                            .flatMap(target -> adminService.unmute(target).thenReturn(target))
                            .switchIfEmpty(messageService.err(channel, "audit.member.unmute.is-not-muted").then(Mono.empty())))
                    .then();
        }
    }
}
