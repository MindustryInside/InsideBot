package inside.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
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
    private final Settings settings;

    public DefaultCommandHandler(@Autowired EntityRetriever entityRetriever,
                                 @Autowired MessageService messageService,
                                 @Autowired CommandHolder commandHolder,
                                 @Autowired Settings settings){
        this.entityRetriever = entityRetriever;
        this.messageService = messageService;
        this.commandHolder = commandHolder;
        this.settings = settings;
    }

    @Override
    public Mono<Void> handleMessage(CommandEnvironment env){
        String message = env.message().getContent();
        Snowflake guildId = env.member().getGuildId();
        Snowflake selfId = env.message().getClient().getSelfId();
        Mono<Guild> guild = env.message().getGuild();

        Mono<String> prefix = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.prefixes().stream()
                        .filter(message::startsWith)
                        .findFirst()));

        Mono<String> mention = Mono.just(message)
                .filter(s -> env.message().getUserMentionIds().contains(selfId))
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
                (prx, text) -> Tuples.of(prx, text.getT1(), text.getT2()));

        Function<String, Mono<Void>> suggestion = s ->
                entityRetriever.getCommandConfigById(guildId, s)
                        .filter(ConfigEntity::isEnabled)
                        .flatMap(c -> Mono.justOrEmpty(commandHolder.getCommandInfo(c.getNames().get(0)))
                                .flatMap(info -> Mono.justOrEmpty(
                                        Stream.concat(Arrays.stream(info.key()), c.getAliases().stream())
                                        .min(Comparator.comparingInt(cmd -> Strings.damerauLevenshtein(s, cmd))))))
                        .switchIfEmpty(Mono.justOrEmpty(commandHolder.getCommandInfoMap().values().stream()
                                .flatMap(commandInfo -> Arrays.stream(commandInfo.key()))
                                .min(Comparator.comparingInt(a -> Strings.damerauLevenshtein(a, s)))))
                .switchIfEmpty(prefix.map(GuildConfig::formatPrefix).flatMap(str ->
                        messageService.err(env, "command.response.unknown", str)).then(Mono.never()))
                .flatMap(s0 -> messageService.err(env, "command.response.found-closest", s0))
                .doFirst(() -> messageService.awaitEdit(env.message().getId()));

        return computedText.flatMap(TupleUtils.function((prx, commandstr, cmdkey) -> Mono.justOrEmpty(commandHolder.getCommand(cmdkey))
                .switchIfEmpty(entityRetriever.getCommandConfigById(guildId, cmdkey)
                        .filter(ConfigEntity::isEnabled)
                        .flatMap(s -> Mono.justOrEmpty(commandHolder.getCommand(s.getNames().get(0)))))
                .switchIfEmpty(suggestion.apply(cmdkey).then(Mono.empty()))
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
                            messageService.awaitEdit(env.message().getId());
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
                                messageService.awaitEdit(env.message().getId());
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
                            env.message().getMessageReference().isEmpty()){ // TODO: strange check, reimplement this using _option types_
                        messageService.awaitEdit(env.message().getId());
                        return messageService.errTitled(env, "command.response.few-arguments.title",
                                argsres, GuildConfig.formatPrefix(prx), cmdkey,
                                messageService.get(env.context(), info.paramText()));
                    }

                    Function<Throwable, Mono<Void>> fallback = t -> Flux.fromIterable(info.permissions())
                            .filterWhen(permission -> env.channel().getEffectivePermissions(selfId)
                                    .map(set -> !set.contains(permission)))
                            .map(permission -> messageService.getEnum(env.context(), permission))
                            .map("• "::concat)
                            .collect(Collectors.joining("\n"))
                            .filter(Predicate.not(String::isBlank))
                            .flatMap(s -> guild.flatMap(Guild::getOwner).flatMap(User::getPrivateChannel)
                                    .flatMap(c -> c.createMessage(EmbedCreateSpec.builder()
                                            .title(messageService.get(env.context(), "message.error.permission-denied.title"))
                                            .description(messageService.format(env.context(),
                                                    "message.error.permission-denied.description", s))
                                            .color(settings.getDefaults().getNormalColor())
                                            .build())))
                            .then();

                    return Mono.just(command)
                            .filterWhen(c -> c.filter(env))
                            .flatMap(c -> c.execute(env, new CommandInteraction(cmdkey, result)))
                            .doFirst(() -> messageService.removeEdit(env.message().getId()))
                            .onErrorResume(t -> t.getMessage() != null &&
                                    (t.getMessage().contains("Missing Access") ||
                                            t.getMessage().contains("Missing Permissions")), fallback);
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
