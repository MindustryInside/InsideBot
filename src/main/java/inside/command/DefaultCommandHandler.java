package inside.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.data.entity.base.ConfigEntity;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.function.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@Service
public class DefaultCommandHandler implements CommandHandler{

    private final EntityRetriever entityRetriever;

    private final MessageService messageService;

    private final CommandHolder commandHolder;

    public DefaultCommandHandler(@Autowired EntityRetriever entityRetriever,
                                 @Autowired MessageService messageService,
                                 @Autowired CommandHolder commandHolder){
        this.entityRetriever = entityRetriever;
        this.messageService = messageService;
        this.commandHolder = commandHolder;
    }

    @Override
    public Mono<Void> handleMessage(CommandEnvironment env){
        String message = env.getMessage().getContent();
        Snowflake guildId = env.getAuthorAsMember().getGuildId();
        Snowflake selfId = env.getMessage().getClient().getSelfId();
        Mono<Guild> guild = env.getMessage().getGuild();

        Mono<String> prefix = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.prefixes().stream()
                        .filter(message::startsWith)
                        .findFirst()));

        Mono<String> mention = Mono.just(message)
                .filter(s -> env.getMessage().getUserMentionIds().contains(selfId))
                .filter(s -> s.startsWith(DiscordUtil.getMemberMention(selfId)) ||
                        s.startsWith(DiscordUtil.getUserMention(selfId)))
                .map(s -> s.startsWith(DiscordUtil.getMemberMention(selfId)) ? DiscordUtil.getMemberMention(selfId) :
                        DiscordUtil.getUserMention(selfId));

        var computedText = prefix.switchIfEmpty(mention).zipWhen(prx -> Mono.just(prx)
                                .filter(message::startsWith)
                                .map(str -> message.substring(str.length()).trim())
                                .zipWhen(str -> Mono.just(str.contains(" ") ? str.substring(0, str.indexOf(" ")) : str)
                                        .map(String::toLowerCase)
                                        .filter(Predicate.not(String::isBlank))),
                        (prx, text) -> Tuples.of(prx, text.getT1(), text.getT2()))
                .cache();

        Mono<Void> suggestion = computedText.map(Tuple3::getT3)
                .flatMap(s -> entityRetriever.getCommandConfigById(guildId, s)
                        .filter(ConfigEntity::isEnabled)
                        .flatMap(c -> Mono.justOrEmpty(commandHolder.getCommandInfo(c.getName()))
                                .flatMap(info -> Mono.justOrEmpty(
                                        Stream.concat(Arrays.stream(info.key()), c.getAliases().stream())
                                        .min(Comparator.comparingInt(cmd -> Strings.levenshtein(s, cmd))))))
                        .switchIfEmpty(Mono.justOrEmpty(commandHolder.getCommandInfoMap().values().stream()
                                .flatMap(commandInfo -> Arrays.stream(commandInfo.key()))
                                .min(Comparator.comparingInt(a -> Strings.levenshtein(a, s))))))
                .switchIfEmpty(prefix.map(GuildConfig::formatPrefix).flatMap(str ->
                        messageService.err(env, "command.response.unknown", str)).then(Mono.never()))
                .flatMap(s -> messageService.err(env, "command.response.found-closest", s))
                .doFirst(() -> messageService.awaitEdit(env.getMessage().getId()));

        return computedText.flatMap(TupleUtils.function((prx, commandstr, cmdkey) -> Mono.justOrEmpty(commandHolder.getCommand(cmdkey))
                .switchIfEmpty(entityRetriever.getCommandConfigById(guildId, cmdkey)
                        .filter(ConfigEntity::isEnabled)
                        .flatMap(s -> Mono.justOrEmpty(commandHolder.getCommand(s.getName()))))
                .switchIfEmpty(suggestion.then(Mono.empty()))
                .flatMap(command -> {
                    CommandInfo info = commandHolder.getCommandInfoMap().get(command);
                    List<CommandOption> result = new ArrayList<>();
                    String argstr = commandstr.contains(" ") ? commandstr.substring(cmdkey.length() + 1) : "";
                    int index = 0;
                    boolean satisfied = false;
                    String argsres = info.paramText().isEmpty() ? "command.response.incorrect-arguments.empty" :
                            "command.response.incorrect-arguments";

                    if(argstr.matches("^(?i)(help|\\?)$")){
                        return command.filter(env).flatMap(bool -> bool
                                ? command.help(env, prx)
                                : Mono.empty());
                    }

                    while(true){
                        if(index >= info.params().length && !argstr.isEmpty()){
                            messageService.awaitEdit(env.getMessage().getId());
                            return messageService.errTitled(env, "command.response.many-arguments.title",
                                    argsres, GuildConfig.formatPrefix(prx), cmdkey,
                                    messageService.get(env.context(), info.paramText()));
                        }

                        if(argstr.isEmpty()){
                            break;
                        }

                        if(info.params()[index].optional() || index >= info.params().length - 1 || info.params()[index + 1].optional()){
                            satisfied = true;
                        }

                        if(info.params()[index].variadic()){
                            result.add(new CommandOption(info.params()[index], argstr));
                            break;
                        }

                        int next = findSpace(argstr);
                        if(next == -1){
                            if(!satisfied){
                                messageService.awaitEdit(env.getMessage().getId());
                                return messageService.errTitled(env, "command.response.few-arguments.title",
                                        argsres, GuildConfig.formatPrefix(prx),
                                        cmdkey, messageService.get(env.context(), info.paramText()));
                            }
                            result.add(new CommandOption(info.params()[index], argstr));
                            break;
                        }

                        String arg = argstr.substring(0, next);
                        argstr = argstr.substring(arg.length() + 1);
                        if(arg.isBlank()){
                            continue;
                        }
                        result.add(new CommandOption(info.params()[index], arg));

                        index++;
                    }

                    if(!satisfied && info.params().length > 0 && !info.params()[0].optional() &&
                            env.getMessage().getMessageReference().isEmpty()){ // TODO: strange check, reimplement this using _option types_
                        messageService.awaitEdit(env.getMessage().getId());
                        return messageService.errTitled(env, "command.response.few-arguments.title",
                                argsres, GuildConfig.formatPrefix(prx), cmdkey,
                                messageService.get(env.context(), info.paramText()));
                    }

                    Predicate<Throwable> missingAccess = t -> t.getMessage() != null &&
                            (t.getMessage().contains("Missing Access") ||
                                    t.getMessage().contains("Missing Permissions"));

                    Function<Throwable, Mono<Void>> fallback = t -> Flux.fromIterable(info.permissions())
                            .filterWhen(permission -> env.getReplyChannel().cast(GuildMessageChannel.class)
                                    .flatMap(targetChannel -> targetChannel.getEffectivePermissions(selfId))
                                    .map(set -> !set.contains(permission)))
                            .map(permission -> messageService.getEnum(env.context(), permission))
                            .map("• "::concat)
                            .collect(Collectors.joining("\n"))
                            .filter(s -> !s.isBlank())
                            .flatMap(s -> messageService.text(env, String.format("%s%n%n%s",
                                            messageService.get(env.context(), "message.error.permission-denied.title"),
                                            messageService.format(env.context(), "message.error.permission-denied.description", s)))
                                    .onErrorResume(missingAccess, t0 -> guild.flatMap(Guild::getOwner)
                                            .flatMap(User::getPrivateChannel)
                                            .transform(c -> messageService.info(c,
                                                    messageService.get(env.context(), "message.error.permission-denied.title"),
                                                    messageService.format(env.context(), "message.error.permission-denied.description", s)))));

                    return Mono.just(command)
                            .filterWhen(c -> c.filter(env))
                            .flatMap(c -> c.execute(env, new CommandInteraction(cmdkey, result)))
                            .doFirst(() -> messageService.removeEdit(env.getMessage().getId()))
                            .onErrorResume(missingAccess, fallback);
                })));
    }


    // Finds a last space index
    private int findSpace(String text){
        for(int i = 0; i < text.length(); i++){
            char c = text.charAt(i);
            if(Character.isWhitespace(c) && (i + 1 < text.length() && !Character.isWhitespace(text.charAt(i + 1)) ||
                    i - 1 != -1 && !Character.isWhitespace(text.charAt(i - 1)))){
                return i;
            }
        }
        return -1;
    }
}
