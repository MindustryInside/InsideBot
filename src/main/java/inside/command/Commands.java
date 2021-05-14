package inside.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.youtube.*;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.udojava.evalex.*;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.*;
import discord4j.rest.util.Permission;
import discord4j.voice.VoiceConnection;
import inside.Settings;
import inside.audit.*;
import inside.command.model.*;
import inside.data.entity.*;
import inside.data.service.AdminService;
import inside.util.*;
import inside.util.io.*;
import inside.voice.*;
import io.netty.handler.codec.http.HttpMethod;
import org.graalvm.polyglot.*;
import org.joda.time.*;
import org.joda.time.format.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.util.*;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static inside.audit.Attribute.COUNT;
import static inside.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.command.Commands.TransliterationCommand.of;
import static inside.service.MessageService.ok;
import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

public class Commands{

    private Commands(){}

    public static abstract class AdminCommand extends Command{
        @Lazy
        @Autowired
        protected AdminService adminService;

        @Override
        public Mono<Boolean> filter(CommandEnvironment env){
            return adminService.isAdmin(env.getAuthorAsMember());
        }
    }

    public static abstract class TestCommand extends Command{
        @Override
        public Mono<Boolean> filter(CommandEnvironment env){
            return env.getClient().getApplicationInfo()
                    .map(ApplicationInfo::getOwnerId)
                    .map(owner -> owner.equals(env.getAuthorAsMember().getId()));
        }
    }

    @DiscordCommand(key = {"help", "?"}, description = "command.help.description")
    public static class HelpCommand extends Command{
        @Autowired
        private CommandHandler handler;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Snowflake guildId = env.getAuthorAsMember().getGuildId();
            Mono<String> prefix = entityRetriever.getGuildConfigById(guildId)
                    .map(GuildConfig::prefix);

            Collector<Map.Entry<String, List<CommandInfo>>, ImmutableEmbedData.Builder, EmbedData> collector = Collector.of(
                    EmbedData::builder,
                    (builder, entry) -> {
                        StringBuilder builder1 = new StringBuilder();
                        entry.getValue().sort((o1, o2) -> Arrays.compare(o1.text(), o2.text()));
                        for(CommandInfo commandInfo : entry.getValue()){
                            builder1.append("**");
                            builder1.append(commandInfo.text()[0]);
                            builder1.append("**");
                            if(commandInfo.params().length > 0){
                                builder1.append(" ");
                                builder1.append(messageService.get(env.context(), commandInfo.paramText()));
                            }
                            builder1.append(" - ");
                            builder1.append(messageService.get(env.context(), commandInfo.description()));
                            builder1.append("\n");
                        }

                        builder.addField(EmbedFieldData.builder()
                                .name(entry.getKey())
                                .value(builder1.toString())
                                .inline(false)
                                .build());
                    },
                    (builder, builder2) -> builder,
                    ImmutableEmbedData.Builder::build);

            return Flux.fromIterable(handler.commandList())
                    .filterWhen(commandInfo -> handler.commands().get(commandInfo.text()).filter(env))
                    .collect(Collectors.groupingBy(c -> messageService.get(env.context(), handler.commands().get(c.text())
                            .getClass().getSuperclass().getCanonicalName())))
                    .flatMapMany(map -> Flux.fromIterable(map.entrySet())
                            .sort(Map.Entry.comparingByKey()))
                    .collect(collector)
                    .flatMap(data -> prefix.flatMap(str -> messageService.info(env.getReplyChannel(), spec -> spec.from(data)
                            .setTitle(messageService.get(env.context(), "command.help"))
                            .setDescription(messageService.format(env.context(), "command.help.disclaimer.prefix", str)
                                    .concat("\n" + messageService.get(env.context(), "command.help.disclaimer.user"))
                                    .concat("\n" + messageService.get(env.context(), "command.help.disclaimer.help"))))));
        }
    }

    @DiscordCommand(key = "ping", description = "command.ping.description")
    public static class PingCommand extends Command{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            long start = System.currentTimeMillis();
            return env.getReplyChannel()
                    .flatMap(channel -> channel.createMessage(
                            messageService.get(env.context(), "command.ping.testing")))
                    .flatMap(message -> message.edit(spec -> spec.setContent(
                            messageService.format(env.context(), "command.ping.completed",
                                    System.currentTimeMillis() - start))))
                    .then();
        }
    }

    @DiscordCommand(key = "diff", params = "command.diff.params", description = "command.diff.description")
    public static class DiffCommand extends Command{
        private final Differ differ = new Differ();

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Optional<Snowflake> message0 = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Optional<Snowflake> message1 = interaction.getOption(1)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            return Mono.justOrEmpty(message0)
                    .switchIfEmpty(messageService.err(env.getReplyChannel(), "command.diff.incorrect-id").then(Mono.empty()))
                    .flatMap(messageId -> env.getReplyChannel().flatMap(channel -> channel.getMessageById(messageId)))
                    .map(Message::getContent)
                    .zipWith(Mono.justOrEmpty(message1)
                            .switchIfEmpty(messageService.err(env.getReplyChannel(), "command.diff.incorrect-id").then(Mono.empty()))
                            .flatMap(messageId -> env.getReplyChannel().flatMap(channel -> channel.getMessageById(messageId)))
                            .map(Message::getContent))
                    .flatMap(function((m0, m1) -> messageService.text(env.getReplyChannel(), String.format("```diff%n%s%n```",
                            differ.getPatch(m0, m1).stream()
                                    .map(Differ.Patch::toString)
                                    .collect(Collectors.joining())))));
        }
    }

    @DiscordCommand(key = "js", params = "command.javascript.params", description = "command.javascript.description")
    public static class JsCommand extends Command{
        private static final List<String> blacklist = List.of(
                ".awt", ".net.", "beans", "channels", "classloader", "compiler", "exec", "file",
                "files", "http", "inside.insidebot", "invoke", "java.net", "javax", "jdk", "oracle", "org.", "org.", "process", "reflect",
                "rmi", "runtime", "security", "socket", "sql", "sun.", "system",
                "thread"
        );

        public static boolean allowClass(String type){
            return blacklist.stream().noneMatch(s -> type.toLowerCase(Locale.ROOT).contains(s));
        }

        private final ReusableByteOutputStream out = new ReusableByteOutputStream();

        private final inside.util.Lazy<Context> context = inside.util.Lazy.of(() -> Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(JsCommand::allowClass)
                .allowAllAccess(false)
                .out(out)
                .build());

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String code = interaction.getOption("code")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<String> exec = Mono.fromCallable(() -> {
                String s = context.get().eval("js", code).toString();
                String s0 = out.toString(StandardCharsets.UTF_8);
                if(s.equals("undefined") && !s0.isEmpty()){
                    s = s0;
                    out.reset();
                }
                return s;
            });

            return exec.publishOn(Schedulers.boundedElastic()).onErrorResume(t -> true,
                    t -> messageService.error(env.getReplyChannel(), "command.javascript.script-error",
                            String.format("```%n%s%n```", t.getMessage())).then(Mono.empty()))
                    .flatMap(it -> messageService.text(env.getReplyChannel(), String.format("```js%n%s%n```",
                            MessageUtil.substringTo(it, 1000))));
        }
    }

    @DiscordCommand(key = {"base64", "b64"}, params = "command.base64.params", description = "command.base64.description")
    public static class Base64Command extends Command{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            boolean encode = interaction.getOption("encode/decode")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(str -> str.matches("(?i)enc(ode)?"))
                    .orElse(false);

            String text = interaction.getOption("text")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<String> result = Mono.fromCallable(() ->
                    encode ? Base64Coder.encodeString(text) : Base64Coder.decodeString(text));

            return result.onErrorResume(t -> t instanceof IllegalArgumentException,
                    t -> messageService.err(env.getReplyChannel(), t.getMessage()).then(Mono.empty()))
                    .flatMap(str -> messageService.text(env.getReplyChannel(),
                            MessageUtil.substringTo(str, Message.MAX_CONTENT_LENGTH)));
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.base64.help",
                            GuildConfig.formatPrefix(prefix)));
        }
    }

    @DiscordCommand(key = "emoji", params = "command.emoji.params", description = "command.emoji.description")
    public static class EmojiCommand extends Command{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();
            String text = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            return env.getAuthorAsMember().getGuild()
                    .flatMapMany(Guild::getEmojis)
                    .filter(emoji -> emoji.asFormat().equals(text)).next()
                    .switchIfEmpty(messageService.err(channel, "command.emoji.not-found").then(Mono.empty()))
                    .flatMap(emoji -> messageService.info(channel, embed -> embed.setImage(emoji.getImageUrl() + "?size=512")
                            .setFooter(messageService.format(env.context(), "common.id", emoji.getId().asString()), null)
                            .setDescription(messageService.format(env.context(), "command.emoji.text", emoji.getName(), emoji.asFormat()))));
        }
    }

    @DiscordCommand(key = "avatar", params = "command.avatar.params", description = "command.avatar.description")
    public static class AvatarCommand extends Command{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();
            Snowflake targetId = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake)
                    .orElse(env.getAuthorAsMember().getId());

            return Mono.justOrEmpty(targetId).flatMap(id -> env.getClient()
                    .withRetrievalStrategy(EntityRetrievalStrategy.REST).getUserById(id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .flatMap(user -> messageService.info(channel, embed -> embed.setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(env.context(), "command.avatar.text", user.getUsername(),
                                    DiscordUtil.getUserMention(user.getId())))));
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.avatar.help",
                            GuildConfig.formatPrefix(prefix)));
        }
    }

    @DiscordCommand(key = "math", params = "command.math.params", description = "command.math.description")
    public static class MathCommand extends Command{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String text = interaction.getOption("math expression")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<BigDecimal> result = Mono.fromCallable(() -> {
                Expression exp = new Expression(text);
                exp.addOperator(shiftRightOperator);
                exp.addOperator(shiftLeftOperator);
                return exp.eval();
            });

            return result.publishOn(Schedulers.boundedElastic())
                    .onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException,
                    t -> messageService.error(env.getReplyChannel(), "command.math.error.title", t.getMessage()).then(Mono.empty()))
                    .flatMap(decimal -> messageService.text(env.getReplyChannel(),
                            MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.math.help",
                            GuildConfig.formatPrefix(prefix)));
        }

        public static final LazyOperator shiftRightOperator = new AbstractOperator(">>", 30, true){
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2){
                return v1.movePointRight(v2.toBigInteger().intValue());
            }
        };

        public static final LazyOperator shiftLeftOperator = new AbstractOperator("<<", 30, true){
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2){
                return v1.movePointLeft(v2.toBigInteger().intValue());
            }
        };
    }

    @DiscordCommand(key = "status", params = "command.status.params", description = "command.status.description")
    public static class StatusCommand extends TestCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String activity = interaction.getOption("status")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(String::toLowerCase)
                    .orElse("");

            return switch(activity){
                case "online" -> env.getClient().updatePresence(ClientPresence.online());
                case "dnd" -> env.getClient().updatePresence(ClientPresence.doNotDisturb());
                case "idle" -> env.getClient().updatePresence(ClientPresence.idle());
                case "invisible" -> env.getClient().updatePresence(ClientPresence.invisible());
                default -> messageService.err(env.getReplyChannel(), "command.status.unknown-presence");
            };
        }
    }

    @DiscordCommand(key = "rm-cmd", params = "command.rm-cmd.params", description = "command.rm-cmd.description")
    public static class RemoveCommand extends TestCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String commandName = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<Long> applicationId = env.getClient().rest()
                    .getApplicationId();

            return applicationId.flatMap(id -> env.getClient().rest().getApplicationService()
                    .getGlobalApplicationCommands(id)
                    .filter(command -> command.name().equalsIgnoreCase(commandName)).next()
                    .switchIfEmpty(messageService.err(env.getReplyChannel(), "command.rm-cmd.unknown-command").then(Mono.empty()))
                    .flatMap(command -> env.getClient().rest().getApplicationService()
                            .deleteGlobalApplicationCommand(id, Snowflake.asLong(command.id()))))
                    .then(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = "r", params = "command.text-layout.params", description = "command.text-layout.description")
    public static class TextLayoutCommand extends Command{
        private static final String[] engPattern;
        private static final String[] rusPattern;

        static{
            String eng = "Q-W-E-R-T-Y-U-I-O-P-A-S-D-F-G-H-J-K-L-Z-X-C-V-B-N-M";
            String rus = "Й-Ц-У-К-Е-Н-Г-Ш-Щ-З-Ф-Ы-В-А-П-Р-О-Л-Д-Я-Ч-С-М-И-Т-Ь";
            engPattern = (eng + "-" + eng.toLowerCase() + "-\\^-:-\\$-@-&-~-`-\\{-\\[-\\}-\\]-\"-'-<->-;-\\?-\\/-\\.-,-#").split("-");
            rusPattern = (rus + "-" + rus.toLowerCase() + "-:-Ж-;-\"-\\?-Ё-ё-Х-х-Ъ-ъ-Э-э-Б-Ю-ж-,-\\.-ю-б-№").split("-");
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            boolean en = interaction.getOption("from ru/en")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map("en"::equalsIgnoreCase)
                    .orElse(false);

            String text = interaction.getOption("text")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            return messageService.text(env.getReplyChannel(), en ? text2rus(text) : text2eng(text));
        }

        public static String text2rus(String text){
            for(int i = 0; i < engPattern.length; i++){
                text = text.replaceAll(engPattern[i], rusPattern[i]);
            }
            return text;
        }

        public static String text2eng(String text){
            for(int i = 0; i < rusPattern.length; i++){
                text = text.replaceAll(rusPattern[i], engPattern[i]);
            }
            return text;
        }
    }

    @DiscordCommand(key = "1337", params = "command.1337.params", description = "command.1337.description")
    public static class LeetSpeakCommand extends Command{
        public static final Map<String, String> rusLeetSpeak;
        public static final Map<String, String> engLeetSpeak;

        static{
            rusLeetSpeak = of(
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

            engLeetSpeak = of(
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
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            boolean ru = interaction.getOption("ru/en")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map("ru"::equalsIgnoreCase)
                    .orElse(false);

            String text = interaction.getOption("text")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(str -> MessageUtil.substringTo(leeted(str, ru), Message.MAX_CONTENT_LENGTH))
                    .orElse("");

            return messageService.text(env.getReplyChannel(), text);
        }

        public static String leeted(String text, boolean russian){
            Map<String, String> map = russian ? rusLeetSpeak : engLeetSpeak;
            UnaryOperator<String> get = s -> {
                String result = Optional.ofNullable(map.get(s.toLowerCase()))
                        .orElse(map.keySet().stream()
                                .filter(s::equalsIgnoreCase)
                                .findFirst().orElse(""));

                return s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result;
            };

            int len = text.length();
            if(len == 1){
                return get.apply(text);
            }

            StringBuilder result = new StringBuilder();
            for(int i = 0; i < len; ){
                String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
                String leeted = get.apply(c);
                if(Strings.isEmpty(leeted)){
                    leeted = get.apply(c.charAt(0) + "");
                    result.append(Strings.isEmpty(leeted) ? c.charAt(0) : leeted);
                    i++;
                }else{
                    result.append(leeted);
                    i += 2;
                }
            }
            return result.toString();
        }
    }

    @DiscordCommand(key = "tr", params = "command.transliteration.params", description = "command.transliteration.description")
    public static class TransliterationCommand extends Command{
        public static final Map<String, String> translit;

        static{
            translit = of(
                    "a", "а", "b", "б", "v", "в", "g", "г",
                    "d", "д", "e", "е", "yo", "ё", "zh", "ж",
                    "z", "з", "i", "и", "j", "й", "k", "к",
                    "l", "л", "m", "м", "n", "н", "o", "о",
                    "p", "п", "r", "р", "s", "с", "t", "т",
                    "u", "у", "f", "ф", "h", "х", "x", "кс",
                    "ts", "ц", "ch", "ч", "sh", "ш", "sh'", "щ",
                    "\\`", "ъ", "y'", "ы", "'", "ь", "e\\`", "э",
                    "yu", "ю", "ya", "я", "iy", "ий"
            );
        }

        @SuppressWarnings("unchecked")
        public static <K, V> Map<K, V> of(Object... values){
            Map<K, V> map = new HashMap<>();

            for(int i = 0; i < values.length / 2; ++i){
                map.put((K)values[i * 2], (V)values[i * 2 + 1]);
            }

            return Map.copyOf(map);
        }

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String translited = interaction.getOption("text")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(str -> MessageUtil.substringTo(translit(str), Message.MAX_CONTENT_LENGTH))
                    .orElse("");

            return messageService.text(env.getReplyChannel(), translited);
        }

        public static String translit(String text){
            UnaryOperator<String> get = s -> {
                String result = Optional.ofNullable(translit.get(s.toLowerCase()))
                        .orElse(translit.keySet().stream()
                                .filter(s::equalsIgnoreCase)
                                .findFirst().orElse(""));

                return s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result;
            };

            int len = text.length();
            if(len == 1){
                return get.apply(text);
            }

            StringBuilder result = new StringBuilder();
            for(int i = 0; i < len; ){
                String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
                String translited = get.apply(c);
                if(Strings.isEmpty(translited)){
                    translited = get.apply(c.charAt(0) + "");
                    result.append(Strings.isEmpty(translited) ? c.charAt(0) : translited);
                    i++;
                }else{
                    result.append(translited);
                    i += 2;
                }
            }
            return result.toString();
        }
    }

    @DiscordCommand(key = "prefix", params = "command.settings.prefix.params", description = "command.settings.prefix.description")
    public static class PrefixCommand extends Command{
        @Autowired
        private AdminService adminService;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member member = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            String prefix = interaction.getOption("prefix")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            return entityRetriever.getGuildConfigById(member.getGuildId())
                    .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isAdmin(member))
                    .switchIfEmpty(messageService.err(channel, "command.owner-only").then(Mono.empty()))
                    .flatMap(guildConfig -> Mono.defer(() -> {
                        guildConfig.prefix(prefix);
                        return messageService.text(channel, "command.settings.prefix.update", guildConfig.prefix())
                                .and(entityRetriever.save(guildConfig));
                    }));
        }
    }

    @DiscordCommand(key = "timezone", params = "command.settings.timezone.params", description = "command.settings.timezone.description")
    public static class TimezoneCommand extends Command{
        @Autowired
        private AdminService adminService;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member member = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            boolean present = interaction.getOption("timezone").isPresent();

            DateTimeZone timeZone = interaction.getOption("timezone")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(TimezoneCommand::findTimeZone)
                    .orElse(null);

            String str =  interaction.getOption("timezone")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElse("");

            return entityRetriever.getGuildConfigById(member.getGuildId())
                    .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isAdmin(member).map(bool -> bool && present))
                    .flatMap(guildConfig -> Mono.defer(() -> {
                        if(timeZone == null){
                            String suggest = Strings.findClosest(DateTimeZone.getAvailableIDs(), str);

                            if(suggest != null){
                                return messageService.err(channel, "command.config.unknown-timezone.suggest", suggest);
                            }
                            return messageService.err(channel, "command.config.unknown-timezone");
                        }

                        guildConfig.timeZone(timeZone);
                        return Mono.deferContextual(ctx -> messageService.text(channel, "command.settings.timezone.update", ctx.<Locale>get(KEY_TIMEZONE)))
                                .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone))
                                .and(entityRetriever.save(guildConfig));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(present ?
                            messageService.err(channel, "command.owner-only").then(Mono.empty()) :
                            messageService.text(channel, "command.settings.timezone.current", env.context().<Locale>get(KEY_TIMEZONE)).then(Mono.empty()))
                    .then(Mono.empty());
        }

        @Nullable
        public static DateTimeZone findTimeZone(String id){
            return Try.ofCallable(() -> DateTimeZone.forID(id)).orElse(null);
        }
    }

    @DiscordCommand(key = "locale", params = "command.settings.locale.params", description = "command.settings.locale.description")
    public static class LocaleCommand extends Command{
        @Autowired
        private AdminService adminService;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member member = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            boolean present = interaction.getOption("locale").isPresent();

            Locale locale = interaction.getOption("locale")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(LocaleUtil::get)
                    .orElse(null);

            return entityRetriever.getGuildConfigById(member.getGuildId())
                    .switchIfEmpty(entityRetriever.createGuildConfig(member.getGuildId()))
                    .filterWhen(guildConfig -> adminService.isOwner(member).map(bool -> bool && present))
                    .flatMap(guildConfig -> Mono.defer(() -> {
                        if(locale == null){
                            String all = LocaleUtil.locales.values().stream()
                                    .map(Locale::toString)
                                    .collect(Collectors.joining(", "));

                            return messageService.text(channel, "command.config.unknown-locale", all);
                        }

                        guildConfig.locale(locale);
                        return Mono.deferContextual(ctx -> messageService.text(channel, "command.settings.locale.update", ctx.<Locale>get(KEY_LOCALE)))
                                .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale))
                                .and(entityRetriever.save(guildConfig));
                    }).thenReturn(guildConfig))
                    .switchIfEmpty(present ?
                            messageService.err(channel, "command.owner-only").then(Mono.empty()) :
                            messageService.text(channel, "command.settings.locale.current", env.context().<Locale>get(KEY_LOCALE)).then(Mono.empty()))
                    .then(Mono.empty());
        }
    }

    @DiscordCommand(key = "mute", params = "command.admin.mute.params", description = "command.admin.mute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public static class MuteCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();

            Member author = env.getAuthorAsMember();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Snowflake guildId = author.getGuildId();

            DateTime delay = interaction.getOption("delay")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asDateTime)
                    .orElse(null);

            if(delay == null){
                return messageService.err(channel, "message.error.invalid-time");
            }

            String reason = interaction.getOption("reason")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(String::trim)
                    .orElse(null);

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .filter(adminConfig -> adminConfig.muteRoleID().isPresent())
                    .switchIfEmpty(messageService.err(channel, "command.disabled.mute").then(Mono.empty()))
                    .flatMap(ignored -> Mono.justOrEmpty(targetId)).flatMap(id -> env.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .filterWhen(member -> BooleanUtils.not(adminService.isMuted(member)))
                    .switchIfEmpty(messageService.err(channel, "command.admin.mute.already-muted").then(Mono.never()))
                    .filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                            .map(function((admin, owner) -> !(admin && !owner))))
                    .switchIfEmpty(messageService.err(channel, "command.admin.user-is-admin").then(Mono.empty()))
                    .flatMap(member -> Mono.defer(() -> {
                        if(author.equals(member)){
                            return messageService.err(channel, "command.admin.mute.self-user");
                        }

                        if(reason != null && !reason.isBlank() && reason.length() >= 512){
                            return messageService.err(channel, "common.string-limit", 512);
                        }

                        return adminService.mute(author, member, delay, reason)
                                .and(env.getMessage().addReaction(ok));
                    }));
        }
    }

    @DiscordCommand(key = "delete", params = "command.admin.delete.params", description = "command.admin.delete.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY})
    public static class DeleteCommand extends AdminCommand{
        @Autowired
        private Settings settings;

        @Autowired
        private AuditService auditService;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member author = env.getAuthorAsMember();
            Mono<TextChannel> reply = env.getReplyChannel().cast(TextChannel.class);

            Optional<String> arg = interaction.getOption("count")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString);

            if(arg.filter(MessageUtil::canParseInt).isEmpty()){
                return messageService.err(reply, "command.incorrect-number");
            }

            long number = arg.map(Strings::parseLong).orElse(0L);
            if(number > settings.getDiscord().getMaxClearedCount()){
                return messageService.err(reply, "common.limit-number", settings.getDiscord().getMaxClearedCount());
            }

            StringBuffer result = new StringBuffer();
            Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                    .withLocale(env.context().get(KEY_LOCALE))
                    .withZone(env.context().get(KEY_TIMEZONE));

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

            Mono<Void> history = reply.flatMapMany(channel -> channel.getMessagesBefore(env.getMessage().getId())
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .doOnNext(member -> appendInfo.accept(message, member))
                            .flatMap(ignored -> entityRetriever.deleteMessageInfoById(message.getId()))
                            .thenReturn(message))
                    .transform(messages -> number > 1 ? channel.bulkDeleteMessages(messages).then() :
                            messages.next().flatMap(Message::delete).then()))
                    .then();

            Mono<Void> log =  reply.flatMap(channel -> auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withChannel(channel)
                    .withAttribute(COUNT, number)
                    .withAttachment(MESSAGE_TXT, ReusableByteInputStream.ofString(result.toString()))
                    .save());

            return history.then(log).and(env.getMessage().addReaction(ok));
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.admin.delete.help",
                            GuildConfig.formatPrefix(prefix)));
        }
    }

    @DiscordCommand(key = "warn", params = "command.admin.warn.params", description = "command.admin.warn.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public static class WarnCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member author = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            String reason = interaction.getOption("reason")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(String::trim)
                    .orElse(null);

            Snowflake guildId = author.getGuildId();

            return Mono.justOrEmpty(targetId).flatMap(id -> env.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.never()))
                    .filterWhen(target -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author))
                            .map(function((admin, owner) -> !(admin && !owner))))
                    .switchIfEmpty(messageService.err(channel, "command.admin.user-is-admin").then(Mono.empty()))
                    .flatMap(member -> {
                        if(author.equals(member)){
                            return messageService.err(channel, "command.admin.warn.self-user");
                        }

                        if(!Strings.isEmpty(reason) && reason.length() >= 512){
                            return messageService.err(channel, "common.string-limit", 512);
                        }

                        Mono<Void> warnings = Mono.defer(() -> adminService.warnings(member).count()).flatMap(count -> {
                            Mono<Void> message = messageService.text(channel, "command.admin.warn", member.getUsername(), count);

                            // TODO: test
                            Mono<AdminConfig> config = entityRetriever.getAdminConfigById(guildId)
                                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId));
                            return message.then(config.filter(adminConfig -> count >= adminConfig.maxWarnCount())
                                    .flatMap(ignored -> author.getGuild().flatMap(guild ->
                                            guild.ban(member.getId(), spec -> spec.setDeleteMessageDays(0)))));
                        });

                        return adminService.warn(author, member, reason).then(warnings);
                    });
        }
    }

    @DiscordCommand(key = "softban", params = "command.admin.softban.params", description = "command.admin.softban.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS})
    public static class SoftbanCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member author = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Optional<String> days = interaction.getOption("delete days")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString);

            if(days.isPresent() && days.filter(MessageUtil::canParseInt).isEmpty()){
                return messageService.err(env.getReplyChannel(), "command.admin.softban.incorrect-delay");
            }

            int deleteDays = days.map(Strings::parseInt).orElse(0);
            if(deleteDays > 7){
                PeriodFormatter formatter = PeriodFormat.wordBased(env.context().get(KEY_LOCALE));
                return messageService.err(env.getReplyChannel(), "command.admin.softban.delay-limit",
                        formatter.print(Days.SEVEN));
            }

            String reason = interaction.getOption("reason")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .map(String::trim)
                    .orElse(null);

            Snowflake guildId = author.getGuildId();

            return Mono.justOrEmpty(targetId).flatMap(id -> env.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.never()))
                    .filterWhen(target -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author))
                            .map(function((admin, owner) -> !(admin && !owner))))
                    .switchIfEmpty(messageService.err(channel, "command.admin.user-is-admin").then(Mono.empty()))
                    .flatMap(member -> member.getGuild().flatMap(guild -> guild.ban(member.getId(), spec -> spec.setReason(reason)
                            .setDeleteMessageDays(deleteDays)))
                            .then(member.getGuild().flatMap(guild -> guild.unban(member.getId()))))
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = "warnings", params = "command.admin.warnings.params", description = "command.admin.warnings.description")
    public static class WarningsCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Snowflake guildId = env.getAuthorAsMember().getGuildId();

            DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                    .withLocale(env.context().get(KEY_LOCALE))
                    .withZone(env.context().get(KEY_TIMEZONE));

            Collector<Tuple2<Long, AdminAction>, ImmutableEmbedData.Builder, EmbedData> collector = Collector.of(EmbedData::builder,
                    (spec, tuple) -> {
                        long index = tuple.getT1();
                        AdminAction warn = tuple.getT2();
                        String value = String.format("%s%n%s",
                                messageService.format(env.context(), "common.admin", warn.admin().effectiveName()),
                                messageService.format(env.context(), "common.reason", warn.reason()
                                        .orElse(messageService.get(env.context(), "common.not-defined"))));

                        EmbedFieldData field = EmbedFieldData.builder()
                                .name(String.format("%2s. %s", index + 1, formatter.print(warn.timestamp())))
                                .value(value)
                                .inline(true)
                                .build();

                        spec.addField(field);
                    },
                    (builder0, builder1) -> builder0, /* non-mergable */
                    ImmutableEmbedData.Builder::build);

            return Mono.justOrEmpty(targetId).filterWhen(id -> env.getClient().getMemberById(guildId, id).hasElement())
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .flatMapMany(id -> adminService.warnings(guildId, id))
                    .switchIfEmpty(messageService.text(channel, "command.admin.warnings.empty").then(Mono.never()))
                    .limitRequest(21).index().collect(collector)
                    .zipWith(env.getClient().getMemberById(guildId, targetId.orElseThrow(AssertionError::new)))
                    .flatMap(function((embed, target) -> messageService.info(channel, spec -> spec.from(embed)
                            .setTitle(messageService.format(env.context(), "command.admin.warnings.title", target.getDisplayName())))));
        }
    }

    @DiscordCommand(key = "unwarn", params = "command.admin.unwarn.params", description = "command.admin.unwarn.description")
    public static class UnwarnCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Member author = env.getAuthorAsMember();
            Mono<MessageChannel> channel = env.getReplyChannel();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Snowflake guildId = env.getAuthorAsMember().getGuildId();

            Optional<String> index = interaction.getOption("index")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString);

            if(index.filter(MessageUtil::canParseInt).isEmpty()){
                return messageService.err(channel, "command.incorrect-number");
            }

            return Mono.justOrEmpty(targetId).flatMap(id -> env.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.never()))
                    .filterWhen(target -> adminService.isOwner(author).map(owner -> !target.equals(author) || owner))
                    .switchIfEmpty(messageService.err(channel, "command.admin.unwarn.permission-denied").then(Mono.empty()))
                    .flatMap(target -> adminService.warnings(target).count().flatMap(count -> {
                        int warn = index.map(Strings::parseInt).orElse(1);
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

    @DiscordCommand(key = "poll", params = "command.poll.params", description = "command.poll.description")
    public static class PollCommand extends Command{

        public static final ReactionEmoji[] emojis;

        static{
            emojis = new ReactionEmoji[]{
                ReactionEmoji.unicode("1\u20E3"),
                ReactionEmoji.unicode("2\u20E3"),
                ReactionEmoji.unicode("3\u20E3"),
                ReactionEmoji.unicode("3\u20E3"),
                ReactionEmoji.unicode("4\u20E3"),
                ReactionEmoji.unicode("5\u20E3"),
                ReactionEmoji.unicode("6\u20E3"),
                ReactionEmoji.unicode("7\u20E3"),
                ReactionEmoji.unicode("8\u20E3"),
                ReactionEmoji.unicode("9\u20E3"),
                ReactionEmoji.unicode("\uD83D\uDD1F")
            };
        }

        @Autowired
        private Settings settings;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();
            Member author = env.getAuthorAsMember();

            String text = interaction.getOption("poll text")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            String title = text.startsWith("**") && text.lastIndexOf("**") != -1 ?
                    text.substring(0, text.lastIndexOf("**") + 2) : null;

            if(title == null || title.equals("**")){
                return messageService.err(env.getReplyChannel(), "command.poll.title").then(Mono.empty());
            }

            String striped = text.substring(title.length());
            int count = striped.split("\\d+.+").length;
            if(count <= 0 || striped.isEmpty()){
                return messageService.err(env.getReplyChannel(), "command.poll.empty-variants");
            }

            if(count > emojis.length){
                return messageService.err(env.getReplyChannel(), "common.limit-number", 10);
            }

            BiFunction<Message, Integer, Mono<Message>> reactions = (message, integer) -> Flux.fromArray(emojis)
                    .limitRequest(integer)
                    .flatMap(message::addReaction)
                    .then(Mono.just(message));

            Mono<Void> message = channel.flatMap(reply -> reply.createMessage(spec -> spec.setEmbed(embed -> embed.setTitle(title)
                    .setColor(settings.getDefaults().getNormalColor())
                    .setDescription(text.substring(title.length()))
                    .setAuthor(author.getUsername(), null, author.getAvatarUrl()))))
                    .flatMap(poll -> Mono.defer(() -> reactions.apply(poll, count)))
                    .then();

            return message.and(env.getMessage().delete());
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.poll.help",
                            GuildConfig.formatPrefix(prefix)));
        }
    }

    @DiscordCommand(key = "qpoll", params = "command.qpoll.params", description = "command.qpoll.description")
    public static class QuickPollCommand extends Command{
        public static final ReactionEmoji up = ReactionEmoji.unicode("\uD83D\uDC4D");
        public static final ReactionEmoji down = ReactionEmoji.unicode("\uD83D\uDC4E");

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();

            String text = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            return channel.flatMap(reply -> reply.createMessage(messageService.format(env.context(),
                    "command.qpoll.text", env.getAuthorAsMember().getUsername(), text)))
                    .flatMap(message1 -> message1.addReaction(up).thenReturn(message1))
                    .flatMap(message1 -> message1.addReaction(down))
                    .then();
        }

        @Override
        public Mono<Void> help(CommandEnvironment env){
            return entityRetriever.getGuildConfigById(env.getAuthorAsMember().getGuildId())
                    .map(GuildConfig::prefix)
                    .flatMap(prefix -> messageService.info(env.getReplyChannel(), "command.help.title", "command.qpoll.help",
                            GuildConfig.formatPrefix(prefix)));
        }
    }

    @DiscordCommand(key = "unmute", params = "command.admin.unmute.params", description = "command.admin.unmute.description",
                    permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES})
    public static class UnmuteCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Mono<MessageChannel> channel = env.getReplyChannel();

            Optional<Snowflake> targetId = interaction.getOption("@user")
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asSnowflake);

            Snowflake guildId = env.getAuthorAsMember().getGuildId();

            return entityRetriever.getAdminConfigById(guildId)
                    .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                    .filter(adminConfig -> adminConfig.muteRoleID().isPresent())
                    .switchIfEmpty(messageService.err(channel, "command.disabled.mute").then(Mono.empty()))
                    .flatMap(ignored -> Mono.justOrEmpty(targetId))
                    .flatMap(id -> env.getClient().getMemberById(guildId, id))
                    .switchIfEmpty(messageService.err(channel, "command.incorrect-name").then(Mono.empty()))
                    .filterWhen(adminService::isMuted)
                    .flatMap(target -> adminService.unmute(target).and(env.getMessage().addReaction(ok)).thenReturn(target))
                    .switchIfEmpty(messageService.err(channel, "audit.member.unmute.is-not-muted").then(Mono.empty()))
                    .then();
        }
    }

    public static abstract class VoiceCommand extends Command{
        @Autowired
        protected VoiceService voiceService;

        @Override
        public Mono<Boolean> filter(CommandEnvironment env){
            return env.getAuthorAsMember().getVoiceState()
                    .flatMap(VoiceState::getChannel)
                    .hasElement();
        }
    }

    @DiscordCommand(key = "pause", description = "command.voice.pause.description")
    public static class VoicePauseCommand extends VoiceCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Snowflake guildId = env.getLocalMember().guildId();

            return Mono.fromRunnable(() -> voiceService.getOrCreate(guildId).getPlayer().setPaused(true))
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = {"leave", "l"}, description = "command.voice.leave.description")
    public static class VoiceLeaveCommand extends VoiceCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Snowflake guildId = env.getLocalMember().guildId();

            return env.getAuthorAsMember().getVoiceState()
                    .flatMap(VoiceState::getChannel)
                    .flatMap(VoiceChannel::getVoiceConnection)
                    .flatMap(VoiceConnection::disconnect)
                    .doFirst(() -> voiceService.getOrCreate(guildId).getPlayer().stopTrack())
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = {"skip", "s"}, description = "command.voice.skip.description")
    public static class VoiceSkipCommand extends VoiceCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Snowflake guildId = env.getLocalMember().guildId();

            VoiceRegistry voiceRegistry = voiceService.getOrCreate(guildId);
            return Mono.fromRunnable(() -> voiceRegistry.getTrackLoader().nextTrack())
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = {"stop", "st"}, description = "command.voice.stop.description")
    public static class VoiceStopCommand extends VoiceCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            Snowflake guildId = env.getLocalMember().guildId();

            VoiceRegistry voiceRegistry = voiceService.getOrCreate(guildId);

            return Mono.fromRunnable(voiceRegistry.getPlayer()::stopTrack)
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = "reconnect", description = "command.voice.reconnect.description")
    public static class VoiceReconnectCommand extends VoiceCommand{
        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            return env.getAuthorAsMember().getVoiceState()
                    .flatMap(VoiceState::getChannel)
                    .flatMap(VoiceChannel::getVoiceConnection)
                    .flatMap(VoiceConnection::reconnect)
                    .and(env.getMessage().addReaction(ok));
        }
    }

    @DiscordCommand(key = {"play", "p"}, params = "command.voice.play.params", description = "command.voice.play.description")
    public static class VoicePlayCommand extends VoiceCommand{
        private static final String api = "https://youtube.googleapis.com/youtube/v3/";

        private static final Logger log = Loggers.getLogger(VoicePlayCommand.class);

        private final HttpClient httpClient = ReactorResources.DEFAULT_HTTP_CLIENT.get();

        @Autowired
        private Settings settings;

        @Override
        public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
            String query = interaction.getOption(0)
                    .flatMap(CommandOption::getValue)
                    .map(OptionValue::asString)
                    .orElseThrow(AssertionError::new);

            VoiceRegistry voiceRegistry = voiceService.getOrCreate(env.getLocalMember().guildId());

            Mono<Void> joinIfNot = Mono.just(env.getAuthorAsMember())
                    .flatMap(Member::getVoiceState)
                    .flatMap(VoiceState::getChannel)
                    .filterWhen(voiceChannel -> BooleanUtils.not(voiceChannel.getVoiceConnection().hasElement()))
                    .flatMap(channel -> channel.join(spec -> spec.setProvider(voiceRegistry.getAudioProvider()))
                            .flatMap(connection -> {
                                Publisher<Boolean> voiceStateCounter = channel.getVoiceStates()
                                        .count()
                                        .map(count -> 1L == count);

                                Mono<Void> onDelay = Mono.delay(Duration.ofSeconds(10L))
                                        .filterWhen(ignored -> voiceStateCounter)
                                        .switchIfEmpty(Mono.never())
                                        .then();

                                Mono<Void> onEvent = channel.getClient().getEventDispatcher().on(VoiceStateUpdateEvent.class)
                                        .filter(event -> event.getOld().flatMap(VoiceState::getChannelId)
                                                .map(channel.getId()::equals).orElse(false))
                                        .filterWhen(ignored -> voiceStateCounter)
                                        .next()
                                        .then();

                                return Mono.firstWithSignal(onDelay, onEvent).then(connection.disconnect());
                            }))
                    .then();

            AudioLoadResultHandler loadResultHandler = new FunctionalResultHandler(
                    voiceRegistry.getTrackLoader()::queue, playlist -> voiceRegistry.getTrackLoader()
                    .queue(Optional.ofNullable(playlist.getSelectedTrack())
                            .orElse(playlist.getTracks().get(0))),
                    () -> messageService.err(env.getReplyChannel(), "command.voice.play.not-found")
                            .contextWrite(env.context()).subscribe(), // ...
                    t -> log.error("Failed to load sound track", t));

            return joinIfNot.and(Mono.fromRunnable(() -> voiceService.getAudioPlayerManager()
                    .loadItemOrdered(voiceRegistry, query, loadResultHandler)));
        }

        private Mono<AudioPlaylist> search(String query, int maxResults, YoutubeAudioSourceManager sourceManager){
            return httpClient.request(HttpMethod.GET)
                    .uri(api + "search" + params(Map.of(
                            "key", settings.getDiscord().getYoutubeApiKey(),
                            "type", "video",
                            "maxResults", maxResults,
                            "q", query
                    )))
                    .responseSingle((res, buf) -> buf.asString().map(JacksonUtil::toJsonNode))
                    .flatMap(node -> {
                        JsonNode items = node.get("items");
                        List<String> ids = new ArrayList<>(maxResults);
                        for(int i = 0; i < maxResults; i++){
                            JsonNode item = items.get(i);
                            if(item == null) continue;

                            ids.add(item.get("id").get("videoId").asText());
                        }

                        return Flux.fromIterable(ids).flatMap(id -> getVideoFromID(id, sourceManager))
                                .collectList()
                                .map(tracks -> new BasicAudioPlaylist(query, tracks, null, true));
                    });
        }

        private Mono<AudioTrack> getVideoFromID(String id, YoutubeAudioSourceManager sourceManager){
            return httpClient.request(HttpMethod.GET)
                    .uri(api + "videos" + params(Map.of("part", "contentDetails,snippet",
                            "id", id, "key", settings.getDiscord().getYoutubeApiKey())))
                    .responseSingle((res, buf) -> buf.asString().map(JacksonUtil::toJsonNode))
                    .flatMap(node -> Mono.justOrEmpty(Optional.ofNullable(node.get("items"))
                            .map(arr -> arr.get(0))
                            .map(single -> {
                                JsonNode snippet = single.get("snippet");

                                JsonNode contentDetails = single.get("contentDetails");

                                YoutubeVideoInfo videoInfo = YoutubeVideoInfo.builder()
                                        .identifier(id)
                                        .title(snippet.get("channelTitle").asText())
                                        .isStream(!"none".equals(snippet.get("liveBroadcastContent").asText()))
                                        .author(snippet.get("title").asText())
                                        .length(Duration.parse(contentDetails.get("duration").asText()).toMillis())
                                        .uri("https://www.youtube.com/watch?v=" + id)
                                        .build();

                                return new YoutubeAudioTrack(videoInfo, sourceManager);
                            })));
        }

        // I hope this not for long
        private static String params(Map<String, Object> map){
            return map.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(Objects.toString(entry.getValue()), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&", "?", ""));
        }
    }
}
