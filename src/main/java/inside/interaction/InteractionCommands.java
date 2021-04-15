package inside.interaction;

import com.udojava.evalex.Expression;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.Settings;
import inside.command.Commands;
import inside.data.entity.GuildConfig;
import inside.data.service.AdminService;
import inside.event.audit.*;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.DateTimeZone;
import org.joda.time.format.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static inside.event.audit.Attribute.COUNT;
import static inside.event.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.util.ContextUtil.*;

public class InteractionCommands{

    private InteractionCommands(){}

    public static abstract class GuildCommand extends InteractionCommand{
        @Autowired
        protected Settings settings;

        @Override
        public Mono<Boolean> apply(InteractionCommandEnvironment env){
            if(env.event().getInteraction().getMember().isEmpty()){
                return messageService.info(env.event(), "message.error.general.title", "command.interaction.only-guild")
                        .then(Mono.empty());
            }
            return Mono.just(true);
        }
    }

    public static abstract class AdminCommand extends GuildCommand{
        @Lazy
        @Autowired
        protected AdminService adminService;

        @Override
        public Mono<Boolean> apply(InteractionCommandEnvironment env){
            Mono<Boolean> isAdmin = env.event().getInteraction().getMember()
                    .map(adminService::isAdmin)
                    .orElse(Mono.just(false));

            return super.apply(env).filterWhen(bool -> BooleanUtils.and(Mono.just(bool), isAdmin));
        }
    }

    @InteractionDiscordCommand
    public static class SettingsCommand extends AdminCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Snowflake guildId = env.event().getInteraction().getGuildId()
                    .orElseThrow(AssertionError::new);

            Mono<Void> handleCommon = Mono.justOrEmpty(env.event().getInteraction().getCommandInteraction()
                    .getOption("common"))
                    .flatMap(group -> {
                        GuildConfig guildConfig = entityRetriever.getGuildById(guildId);

                        Mono<Void> timezoneCommand = Mono.justOrEmpty(group.getOption("timezone"))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.config.current-timezone",
                                        guildConfig.timeZone()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    DateTimeZone timeZone = Commands.TimezoneCommand.findTimeZone(str);
                                    if(timeZone == null){
                                        String suggest = Strings.findClosest(DateTimeZone.getAvailableIDs(), str);

                                        if(suggest != null){
                                            return messageService.err(env.event(), "command.config.unknown-timezone.suggest", suggest);
                                        }
                                        return messageService.err(env.event(), "command.config.unknown-timezone");
                                    }

                                    guildConfig.timeZone(timeZone);
                                    entityRetriever.save(guildConfig);
                                    return Mono.deferContextual(ctx -> messageService.text(env.event(),
                                            "command.config.timezone-updated", ctx.<Locale>get(KEY_TIMEZONE)))
                                            .contextWrite(ctx -> ctx.put(KEY_TIMEZONE, timeZone));
                                }));

                        Mono<Void> localeCommand = Mono.justOrEmpty(group.getOption("locale"))
                                .switchIfEmpty(timezoneCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.config.current-locale",
                                        guildConfig.locale()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    Locale locale = LocaleUtil.get(str);
                                    if(locale == null){
                                        String all = LocaleUtil.locales.values().stream()
                                                .map(Locale::toString)
                                                .collect(Collectors.joining(", "));

                                        return messageService.text(env.event(), "command.config.unknown-locale", all);
                                    }

                                    guildConfig.locale(locale);
                                    entityRetriever.save(guildConfig);
                                    return Mono.deferContextual(ctx -> messageService.text(env.event(), "command.config.locale-updated",
                                            ctx.<Locale>get(KEY_LOCALE)))
                                            .contextWrite(ctx -> ctx.put(KEY_LOCALE, locale));
                                }));

                        return Mono.justOrEmpty(group.getOption("prefix"))
                                .switchIfEmpty(localeCommand.then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getOption("value")))
                                .switchIfEmpty(messageService.text(env.event(), "command.config.current-prefix",
                                        guildConfig.prefix()).then(Mono.empty()))
                                .flatMap(opt -> Mono.justOrEmpty(opt.getValue())
                                        .map(ApplicationCommandInteractionOptionValue::asString))
                                .flatMap(str -> Mono.defer(() -> {
                                    guildConfig.prefix(str);
                                    entityRetriever.save(guildConfig);
                                    return messageService.text(env.event(), "command.config.prefix-updated", guildConfig.prefix());
                                }));

                    });

            return handleCommon;
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("settings")
                    .description("Configure guild settings.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("common")
                            .description("Different bot settings")
                            .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("prefix")
                                    .description("Configure bot prefix")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New prefix")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("locale")
                                    .description("Configure bot locale")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New locale")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("timezone")
                                    .description("Configure bot time zone")
                                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                                    .addOption(ApplicationCommandOptionData.builder()
                                            .name("value")
                                            .description("New time zone")
                                            .type(ApplicationCommandOptionType.STRING.getValue())
                                            .build())
                                    .build())
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class TextLayoutCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            boolean russian = env.event().getInteraction().getCommandInteraction()
                    .getOption("type")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map("ru"::equalsIgnoreCase)
                    .orElse(false);

            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(str -> russian ? Commands.TextLayoutCommand.text2eng(str) : Commands.TextLayoutCommand.text2rus(str))
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("r")
                    .description("Change text layout.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Text layout type")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("English layout")
                                    .value("en")
                                    .build())
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("Russian layout")
                                    .value("ru")
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Target text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class LeetSpeakCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            boolean russian = env.event().getInteraction().getCommandInteraction()
                    .getOption("type")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(str -> str.equalsIgnoreCase("ru"))
                    .orElse(false);

            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(str -> Commands.LeetSpeakCommand.leeted(str, russian))
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("1337")
                    .description("Translate text into leet speak.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Leet speak type")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("English leet")
                                    .value("en")
                                    .build())
                            .addChoice(ApplicationCommandOptionChoiceData.builder()
                                    .name("Russian leet")
                                    .value("ru")
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Target text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class TransliterationCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            String text = env.event().getInteraction().getCommandInteraction()
                    .getOption("text")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(Commands.TransliterationCommand::translit)
                    .orElse(MessageService.placeholder);

            return env.event().reply(text);
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("tr")
                    .description("Translating text into transliteration.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("text")
                            .description("Translation text")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class DeleteCommand extends AdminCommand{
        @Autowired
        private AuditService auditService;

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Member author = env.event().getInteraction().getMember()
                    .orElseThrow(AssertionError::new);

            Mono<TextChannel> reply = env.getReplyChannel().cast(TextChannel.class);

            long number = env.event().getInteraction().getCommandInteraction()
                    .getOption("count")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong)
                    .orElse(0L);

            if(number <= 0){
                return messageService.err(env.event(), "command.incorrect-number");
            }

            if(number > settings.getDiscord().getMaxClearedCount()){
                return messageService.err(env.event(), "common.limit-number", settings.getDiscord().getMaxClearedCount());
            }

            StringBuffer result = new StringBuffer();
            Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss")
                    .withLocale(env.context().get(KEY_LOCALE))
                    .withZone(env.context().get(KEY_TIMEZONE));

            ReusableByteInputStream input = new ReusableByteInputStream();
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

            Mono<Void> history = reply.flatMapMany(channel -> channel.getLastMessage()
                    .flatMapMany(message -> channel.getMessagesBefore(message.getId()))
                    .limitRequest(number)
                    .sort(Comparator.comparing(Message::getId))
                    .filter(message -> message.getTimestamp().isAfter(limit))
                    .flatMap(message -> message.getAuthorAsMember()
                            .doOnNext(member -> {
                                appendInfo.accept(message, member);
                                messageService.deleteById(message.getId());
                            })
                            .thenReturn(message))
                    .transform(messages -> number > 1 ? channel.bulkDeleteMessages(messages).then() : messages.next().flatMap(Message::delete).then()))
                    .then();

            Mono<Void> log = reply.flatMap(channel -> auditService.log(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                    .withUser(author)
                    .withChannel(channel)
                    .withAttribute(COUNT, number)
                    .withAttachment(MESSAGE_TXT, input.withString(result.toString()))
                    .save());

            return history.then(log).and(env.event().reply("\u2063✅")); // to reduce emoji size
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("delete")
                    .description("Delete some messages.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("count")
                            .description("How many messages to delete")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class AvatarCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            return env.event().getInteraction().getCommandInteraction()
                    .getOption("target")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asUser)
                    .orElse(Mono.just(env.event().getInteraction().getUser()))
                    .flatMap(user -> messageService.info(env.event(), embed -> embed.setImage(user.getAvatarUrl() + "?size=512")
                            .setDescription(messageService.format(env.context(), "command.avatar.text", user.getUsername()))));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("avatar")
                    .description("Get user avatar.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("target")
                            .description("Whose avatar needs to get. By default your avatar")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class PingCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Mono<MessageChannel> reply = env.getReplyChannel();

            long start = System.currentTimeMillis();
            return env.event().acknowledge().then(env.event().getInteractionResponse()
                    .createFollowupMessage(messageService.get(env.context(), "command.ping.testing"))
                    .flatMap(data -> reply.flatMap(channel -> channel.getMessageById(Snowflake.of(data.id())))
                            .flatMap(message -> message.edit(spec -> spec.setContent(messageService.format(env.context(), "command.ping.completed",
                                    System.currentTimeMillis() - start))))))
                    .then();
        }
        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("ping")
                    .description("Get bot ping.")
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class MathCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            String expression = env.event().getInteraction().getCommandInteraction()
                    .getOption("expression")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElseThrow(AssertionError::new);

            Mono<BigDecimal> result = Mono.fromCallable(() -> {
                Expression exp = new Expression(expression).setPrecision(10);
                exp.addOperator(Commands.MathCommand.shiftRightOperator);
                exp.addOperator(Commands.MathCommand.shiftLeftOperator);
                return exp.eval();
            });

            return result.onErrorResume(t -> t instanceof ArithmeticException || t instanceof Expression.ExpressionException,
                    t -> messageService.error(env.event(), "command.math.error.title", t.getMessage()).then(Mono.empty()))
                    .flatMap(decimal -> messageService.text(env.event(), MessageUtil.substringTo(decimal.toString(), Message.MAX_CONTENT_LENGTH)));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("math")
                    .description("Calculate math expression.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("expression")
                            .description("Math expression")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build();
        }
    }
}
