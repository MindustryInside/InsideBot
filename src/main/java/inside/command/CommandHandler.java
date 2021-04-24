package inside.command;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;

@Service
public class CommandHandler{
    private final EntityRetriever entityRetriever;

    private final MessageService messageService;

    private final Map<String, Command> commands = new LinkedHashMap<>();

    private final Map<Command, CommandInfo> commandInfo = new LinkedHashMap<>();

    public CommandHandler(@Autowired EntityRetriever entityRetriever,
                          @Autowired MessageService messageService){
        this.entityRetriever = entityRetriever;
        this.messageService = messageService;
    }

    // TODO: immutable collections
    @Autowired(required = false)
    public void init(List<Command> commands){
        for(Command command : commands){
            CommandInfo info = compile(command);
            this.commands.put(info.text(), command);
            this.commandInfo.put(command, info);
        }
    }

    private CommandInfo compile(Command command){
        DiscordCommand meta = command.getAnnotation();
        // get 'en' parameter text for validation and option search
        String paramText = messageService.get(Context.of(KEY_LOCALE, LocaleUtil.getDefaultLocale()), meta.params());
        String[] psplit = paramText.split("(?<=(\\]|>))\\s+(?=(\\[|<))");
        CommandParam[] params = new CommandParam[0];
        if(!paramText.isBlank()){
            params = new CommandParam[psplit.length];
            boolean hadOptional = false;

            for(int i = 0; i < params.length; i++){
                String param = psplit[i].trim();
                Preconditions.requireState(Strings.isNotEmpty(param), "Malformed param '" + param + "'");

                char l = param.charAt(0), r = param.charAt(param.length() - 1);
                boolean optional, variadic = false;

                if(l == '<' && r == '>'){
                    Preconditions.requireState(!hadOptional, "Can't have non-optional param after optional param!");
                    optional = false;
                }else if(l == '[' && r == ']'){
                    optional = hadOptional = true;
                }else{
                    throw new IllegalArgumentException("Malformed param '" + param + "'");
                }

                String fname = param.substring(1, param.length() - 1);
                if(fname.endsWith("...")){
                    Preconditions.requireState(i != param.length() - 1, "A variadic parameter should be the last parameter!");
                    fname = fname.substring(0, fname.length() - 3);
                    variadic = true;
                }

                params[i] = new CommandParam(fname, optional, variadic);
            }
        }

        return new CommandInfo(meta.key(), meta.params(), meta.description(), params, meta.permissions());
    }

    public Map<String, Command> commands(){
        return commands;
    }

    public Collection<CommandInfo> commandList(){
        return commandInfo.values();
    }

    public Mono<Void> handleMessage(final CommandEnvironment env){
        String message = env.getMessage().getContent();
        Snowflake guildId = env.getAuthorAsMember().getGuildId();
        Mono<Guild> guild = env.getMessage().getGuild();
        Mono<TextChannel> channel = env.getReplyChannel().ofType(TextChannel.class);
        Snowflake selfId = env.getClient().getSelfId();

        Mono<String> prefix = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(GuildConfig::prefix)
                .map(GuildConfig::formatPrefix);

        Mono<String> mention = Mono.just(message)
                .filter(s -> env.getMessage().getUserMentionIds().contains(selfId))
                .filter(s -> message.startsWith(DiscordUtil.getMemberMention(selfId)) || message.startsWith(DiscordUtil.getUserMention(selfId)))
                .map(s -> s.startsWith(DiscordUtil.getMemberMention(selfId)) ? DiscordUtil.getMemberMention(selfId) : DiscordUtil.getUserMention(selfId));

        Mono<Tuple2<String, String>> text = prefix.filter(message::startsWith)
                .switchIfEmpty(mention)
                .map(s -> message.substring(s.length()).trim())
                .zipWhen(s -> Mono.just(s.contains(" ") ? s.substring(0, s.indexOf(" ")) : s).map(String::toLowerCase))
                .cache();

        Mono<Void> suggestion = text.map(Tuple2::getT1).flatMap(commandName -> {
            CommandInfo closest = Strings.findClosest(commandList(), CommandInfo::text, commandName);

            messageService.awaitEdit(env.getMessage().getId());
            if(closest != null){
                return messageService.err(channel, "command.response.found-closest", closest.text());
            }
            return prefix.flatMap(str -> messageService.err(channel, "command.response.unknown", str));
        });

        return text.flatMap(TupleUtils.function((commandstr, cmd) -> Mono.justOrEmpty(commands.get(cmd))
                .switchIfEmpty(suggestion.then(Mono.empty()))
                .flatMap(command -> {
                    CommandInfo info = commandInfo.get(command);
                    List<CommandOption> result = new ArrayList<>();
                    String argstr = commandstr.contains(" ") ? commandstr.substring(cmd.length() + 1) : "";
                    int index = 0;
                    boolean satisfied = false;
                    String argsres = info.paramText().isEmpty() ? "command.response.incorrect-arguments.empty" :
                                     "command.response.incorrect-arguments";

                    if(argstr.matches("(?i)(help|\\?)")){
                        return command.apply(env).flatMap(bool -> bool ? command.help(env) : Mono.empty());
                    }

                    while(true){
                        if(index >= info.params().length && !argstr.isEmpty()){
                            messageService.awaitEdit(env.getMessage().getId());
                            return prefix.flatMap(str -> messageService.error(channel, "command.response.many-arguments.title",
                                    argsres, str, info.text(), messageService.get(env.context(), info.paramText())));
                        }else if(argstr.isEmpty()){
                            break;
                        }

                        if(info.params()[index].optional() || index >= info.params().length - 1 || info.params()[index + 1].optional()){
                            satisfied = true;
                        }

                        if(info.params()[index].variadic()){
                            result.add(new CommandOption(info.params()[index], argstr));
                            break;
                        }

                        int next = argstr.indexOf(" ");
                        if(next == -1){
                            if(!satisfied){
                                messageService.awaitEdit(env.getMessage().getId());
                                return prefix.flatMap(str -> messageService.error(channel, "command.response.few-arguments.title",
                                        argsres, str, info.text(), messageService.get(env.context(), info.paramText())));
                            }
                            result.add(new CommandOption(info.params()[index], argstr));
                            break;
                        }else{
                            String arg = argstr.substring(0, next);
                            argstr = argstr.substring(arg.length() + 1);
                            result.add(new CommandOption(info.params()[index], arg));
                        }

                        index++;
                    }

                    if(!satisfied && info.params().length > 0 && !info.params()[0].optional()){
                        messageService.awaitEdit(env.getMessage().getId());
                        return prefix.flatMap(str -> messageService.error(channel, "command.response.few-arguments.title",
                                argsres, str, info.text(), messageService.get(env.context(), info.paramText())));
                    }

                    Mono<Void> execute = Mono.just(command)
                            .filterWhen(c -> c.apply(env))
                            .flatMap(c -> c.execute(env, new CommandInteraction(cmd, result)))
                            .doFirst(() -> messageService.removeEdit(env.getMessage().getId()));

                    Function<String, Mono<String>> fallback = s -> messageService.text(channel, String.format("%s%n%n%s",
                            messageService.get(env.context(), "message.error.permission-denied.title"),
                            messageService.format(env.context(), "message.error.permission-denied.description", s)))
                            .onErrorResume(t -> t.getMessage().contains("Missing Permissions"), t ->
                                    guild.flatMap(g -> g.getOwner().flatMap(User::getPrivateChannel))
                                            .flatMap(c -> c.createMessage(String.format("%s%n%n%s",
                                                    messageService.get(env.context(), "message.error.permission-denied.title"),
                                                    messageService.format(env.context(), "message.error.permission-denied.description", s))))
                                            .then())
                            .thenReturn(s);

                    return Flux.fromIterable(info.permissions())
                            .filterWhen(permission -> channel.flatMap(targetChannel ->
                                    targetChannel.getEffectivePermissions(selfId))
                                    .map(set -> !set.contains(permission)))
                            .map(permission -> messageService.getEnum(env.context(), permission))
                            .collect(Collectors.joining("\n"))
                            .filter(s -> !s.isBlank())
                            .flatMap(fallback)
                            .switchIfEmpty(execute.then(Mono.empty()))
                            .then();
                })));
    }
}
