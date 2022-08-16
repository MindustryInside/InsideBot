package inside.command;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Configuration;
import inside.data.EntityRetriever;
import inside.data.entity.GuildConfig;
import inside.service.MessageService;
import inside.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static inside.util.MessageUtil.getMemberMention;
import static inside.util.MessageUtil.getUserMention;

public final class CommandHandler {

    private final EntityRetriever entityRetriever;
    private final CommandHolder commandHolder;
    private final MessageService messageService;
    private final Configuration configuration;

    private final Cache<Snowflake, Boolean> waitingMessage = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public CommandHandler(EntityRetriever entityRetriever, CommandHolder commandHolder,
                          MessageService messageService, Configuration configuration) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever);
        this.commandHolder = Objects.requireNonNull(commandHolder);
        this.messageService = Objects.requireNonNull(messageService);
        this.configuration = Objects.requireNonNull(configuration);
    }


    public Mono<Void> handle(CommandEnvironment env) {
        String message = env.message().getContent();
        Snowflake guildId = env.member().getGuildId();
        Snowflake selfId = env.message().getClient().getSelfId();

        Mono<String> prefix = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.prefixes().stream()
                        .filter(message::startsWith)
                        .findFirst()));

        Mono<String> mention = Mono.just(message)
                .filter(s -> env.message().getUserMentionIds().contains(selfId))
                .filter(s -> s.startsWith(getMemberMention(selfId)) ||
                        s.startsWith(getUserMention(selfId)))
                .map(s -> s.startsWith(getMemberMention(selfId)) ? getMemberMention(selfId) :
                        getUserMention(selfId));

        return prefix.switchIfEmpty(mention)
                .flatMap((prx) -> {
                    String striped = message.substring(prx.length()).trim();
                    int commandNameEnd = (commandNameEnd = striped.indexOf(' ', prx.length())) != -1 ? commandNameEnd : striped.length();
                    String commandName = striped.substring(0, commandNameEnd).toLowerCase(Locale.ROOT);
                    int paramTextBegin = striped.indexOf(' ', commandNameEnd);
                    String paramText = paramTextBegin != -1 ? striped.substring(paramTextBegin + 1) : "";

                    Command cmd = commandHolder.getCommand(commandName).orElse(null);
                    if (cmd == null) {
                        awaitEdit(env.message().getId());

                        return Mono.justOrEmpty(commandHolder.getCommandInfoMap().values().stream()
                                        .flatMap(commandInfo -> Arrays.stream(commandInfo.key()))
                                        .min(Comparator.comparingInt(a -> Strings.damerauLevenshtein(a, commandName))))
                                .switchIfEmpty(prefix.map(GuildConfig::formatPrefix).flatMap(str ->
                                        messageService.err(env, "Неизвестная команда. Напишите %shelp для получения списка команд.", str))
                                        .then(Mono.never()))
                                .flatMap(s0 -> messageService.err(env, "Команда не найдена. Может вы имели ввиду \"%s\"?", s0));
                    }

                    CommandInfo info = commandHolder.getCommandInfoMap().get(cmd);
                    String argsres = "Используйте: %s%s" + (info.paramText().isEmpty() ? "" : " *%s*");

                    List<CommandOption> opts = new ArrayList<>(info.params().length);

                    int idx = 0;

                    int quoteBegin = -1;
                    int quoteEnd = -1;
                    boolean satisfied = false;
                    for (int i = 0; i < paramText.length(); i++) {
                        char c = paramText.charAt(i);
                        CommandParam param = info.params()[idx];
                        if (param.optional() || idx >= info.params().length - 1 || info.params()[idx + 1].optional()) {
                            satisfied = true;
                        }

                        if (param.variadic()) {
                            opts.add(new CommandOption(param, paramText.substring(i)));
                            break;
                        }

                        if ((i - 1 == -1 || paramText.charAt(i - 1) != '\\') && (c == '\'' || c == '"')) {
                            if (quoteBegin == -1) {
                                quoteBegin = i;
                            } else {
                                quoteEnd = i;
                            }
                        }

                        String arg = "";
                        if (quoteBegin != -1 && quoteEnd != -1) {
                            arg = paramText.substring(quoteBegin + 1, quoteEnd);
                        } else if (quoteBegin == -1) {
                            int next = findSpace(paramText, i);
                            if (next == -1) {
                                if (!satisfied) {
                                    awaitEdit(env.message().getId());
                                    return messageService.errTitled(env, "Слишком мало аргументов",
                                            argsres, GuildConfig.formatPrefix(prx),
                                            commandName, env.context(), info.paramText());
                                } else {
                                    next = paramText.length();
                                }
                            }

                            arg = paramText.substring(i, next);
                        }

                        if (!arg.isEmpty()) {
                            opts.add(new CommandOption(param, arg));

                            if (++idx >= info.params().length) {
                                if (!satisfied) {
                                    awaitEdit(env.message().getId());

                                    return messageService.errTitled(env, "Слишком много аргументов",
                                            argsres, GuildConfig.formatPrefix(prx), commandName, info.paramText());
                                }

                                break;
                            }
                        }
                    }

                    if (!satisfied && info.params().length > 0 && !info.params()[0].optional()) {
                        awaitEdit(env.message().getId());

                        return messageService.errTitled(env, "Слишком мало аргументов",
                                argsres, GuildConfig.formatPrefix(prx), commandName, info.paramText());
                    }

                    Function<Throwable, Mono<Void>> fallback = t -> Flux.fromIterable(info.permissions())
                            .filterWhen(permission -> env.channel().getEffectivePermissions(selfId)
                                    .map(set -> !set.contains(permission)))
                            .map(c -> messageService.getEnum(env.context(), c))
                            .map("• "::concat)
                            .collect(Collectors.joining("\n"))
                            .filter(Predicate.not(String::isBlank))
                            .flatMap(s -> env.member().getGuild()
                                    .flatMap(Guild::getOwner)
                                    .flatMap(User::getPrivateChannel)
                                    .flatMap(c -> c.createMessage(EmbedCreateSpec.builder()
                                            .title("Недостаточно прав")
                                            .description(String.format("Предоставьте мне эти права для использования этой команды: \n%s", s))
                                            .color(configuration.discord().embedColor())
                                            .build())))
                            .then();

                    return Mono.just(cmd)
                            .filterWhen(c -> c.filter(env))
                            .flatMap(c -> Mono.from(c.execute(env, new CommandInteraction(commandName, opts))).then())
                            .doFirst(() -> removeEdit(env.message().getId()))
                            .onErrorResume(t -> t.getMessage() != null &&
                                    (t.getMessage().contains("Missing Access") ||
                                    t.getMessage().contains("Missing Permissions")), fallback);
                });
    }

    public void awaitEdit(Snowflake messageId) {
        waitingMessage.put(messageId, true);
    }

    public void removeEdit(Snowflake messageId) {
        waitingMessage.invalidate(messageId);
    }

    public boolean isAwaitEdit(Snowflake messageId) {
        return Boolean.TRUE.equals(waitingMessage.getIfPresent(messageId));
    }

    // Finds a last space index
    private static int findSpace(String text, int offset) {
        for (int i = offset; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) && (i + 1 >= text.length() || !Character.isWhitespace(text.charAt(i + 1)))) {
                return i + 1;
            }
        }
        return -1;
    }
}
