package inside.command;

import arc.util.Strings;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import inside.command.model.*;
import inside.data.service.*;
import inside.util.LocaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.KEY_LOCALE;

@Service
public class CommandHandler{

    private final EntityRetriever entityRetriever;

    private final MessageService messageService;

    private final Map<String, Command> commands = new LinkedHashMap<>();

    private final Map<Command, CommandInfo> commandInfos = new LinkedHashMap();

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
            this.commands.put(info.text, command);
            this.commandInfos.put(command, info);
        }
    }

    private CommandInfo compile(Command command){
        DiscordCommand meta = command.getAnnotation();
        String paramText = messageService.get(Context.of(KEY_LOCALE, LocaleUtil.getDefaultLocale()), meta.params()); // get 'en' parameter text
        String[] psplit = paramText.split("(?<=(\\]|>))\\s+(?=(\\[|<))");
        CommandParam[] params = new CommandParam[0];
        if(!paramText.isBlank()){
            params = new CommandParam[psplit.length];
            boolean hadOptional = false;

            for(int i = 0; i < params.length; i++){
                String param = psplit[i].trim();
                if(param.length() <= 2){
                    throw new IllegalArgumentException("Malformed param '" + param + "'");
                }

                char l = param.charAt(0), r = param.charAt(param.length() - 1);
                boolean optional, variadic = false;

                if(l == '<' && r == '>'){
                    if(hadOptional){
                        throw new IllegalArgumentException("Can't have non-optional param after optional param!");
                    }
                    optional = false;
                }else if(l == '[' && r == ']'){
                    optional = true;
                }else{
                    throw new IllegalArgumentException("Malformed param '" + param + "'");
                }

                if(optional){
                    hadOptional = true;
                }

                String fname = param.substring(1, param.length() - 1);
                if(fname.endsWith("...")){
                    if(i != params.length - 1){
                        throw new IllegalArgumentException("A variadic parameter should be the last parameter!");
                    }
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
        return commandInfos.values();
    }

    public Mono<?> handleMessage(final CommandReference ref){
        String message = ref.getMessage().getContent();
        Mono<Guild> guild = ref.getMessage().getGuild();
        Mono<TextChannel> channel = ref.getReplyChannel().ofType(TextChannel.class);
        Mono<User> self = ref.getClient().getSelf();

        String prefix = entityRetriever.prefix(ref.getAuthorAsMember().getGuildId());

        Mono<Tuple2<String, String>> text = Mono.justOrEmpty(prefix)
                .filter(message::startsWith)
                .map(s -> message.substring(s.length()).trim())
                .zipWhen(s -> Mono.justOrEmpty(s.contains(" ") ? s.substring(0, s.indexOf(" ")) : s))
                .cache();

        Mono<Void> suggestion = text.flatMap(t -> {
            int min = 0;
            CommandInfo closest = null;

            for(CommandInfo c : commandList()){
                int dst = Strings.levenshtein(c.text, t.getT1());
                if(dst < 3 && (closest == null || dst < min)){
                    min = dst;
                    closest = c;
                }
            }

            if(closest != null){
                return messageService.err(channel, messageService.format(ref.context(), "command.response.found-closest", closest.text));
            }
            return messageService.err(channel, messageService.format(ref.context(), "command.response.unknown", prefix));
        }).doFirst(() -> messageService.awaitEdit(ref.getMessage().getId()));

        return text.flatMap(TupleUtils.function((commandstr, cmd) -> Mono.defer(() -> commands.containsKey(cmd) ? Mono.just(commands.get(cmd)) : suggestion)
                .ofType(Command.class)
                .flatMap(command -> {
                    CommandInfo commandInfo = commandInfos.get(command);
                    List<String> result = new ArrayList<>();
                    String argstr = commandstr.contains(" ") ? commandstr.substring(cmd.length() + 1) : "";
                    int index = 0;
                    boolean satisfied = false;
                    String argsres = commandInfo.paramText.isEmpty() ? "command.response.incorrect-arguments.empty" : "command.response.incorrect-arguments";

                    while(true){
                        if(index >= commandInfo.params.length && !argstr.isEmpty()){
                            messageService.awaitEdit(ref.getMessage().getId());
                            return messageService.err(channel, messageService.get(ref.context(), "command.response.many-arguments.title"),
                                                      messageService.format(ref.context(), argsres, prefix, commandInfo.text, commandInfo.paramText));
                        }else if(argstr.isEmpty()){
                            break;
                        }

                        if(commandInfo.params[index].optional || index >= commandInfo.params.length - 1 || commandInfo.params[index + 1].optional){
                            satisfied = true;
                        }

                        if(commandInfo.params[index].variadic){
                            result.add(argstr);
                            break;
                        }

                        int next = argstr.indexOf(" ");
                        if(next == -1){
                            if(!satisfied){
                                messageService.awaitEdit(ref.getMessage().getId());
                                return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                                          messageService.format(ref.context(), argsres, prefix, commandInfo.text, commandInfo.paramText));
                            }
                            result.add(argstr);
                            break;
                        }else{
                            String arg = argstr.substring(0, next);
                            argstr = argstr.substring(arg.length() + 1);
                            result.add(arg);
                        }

                        index++;
                    }

                    if(!satisfied && commandInfo.params.length > 0 && !commandInfo.params[0].optional){
                        messageService.awaitEdit(ref.getMessage().getId());
                        return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                                  messageService.format(ref.context(), argsres, prefix, commandInfo.text, commandInfo.paramText));
                    }

                    Mono<String> execute = Mono.just(command)
                            .filterWhen(c -> c.apply(ref))
                            .flatMap(c -> command.execute(ref, result.toArray(new String[0])))
                            .doFirst(() -> messageService.removeEdit(ref.getMessage().getId()))
                            .then(Mono.empty());

                    return Flux.fromIterable(commandInfo.permissions)
                            .filterWhen(permission -> channel.zipWith(self.map(User::getId))
                                    .flatMap(TupleUtils.function((targetChannel, selfId) -> targetChannel.getEffectivePermissions(selfId)))
                                    .map(set -> !set.contains(permission)))
                            .map(permission -> messageService.getEnum(ref.context(), permission))
                            .collect(Collectors.joining("\n"))
                            .filter(s -> !s.isBlank())
                            .flatMap(s -> messageService.text(channel, String.format("%s%n%n%s",
                                    messageService.get(ref.context(), "message.error.permission-denied.title"),
                                    messageService.format(ref.context(), "message.error.permission-denied.description", s)))
                                    .onErrorResume(t -> t.getMessage().contains("Missing Permissions"), t ->
                                            guild.flatMap(g -> g.getOwner().flatMap(User::getPrivateChannel))
                                                    .flatMap(c -> c.createMessage(String.format("%s%n%n%s",
                                                            messageService.get(ref.context(), "message.error.permission-denied.title"),
                                                            messageService.format(ref.context(), "message.error.permission-denied.description", s))))
                                                    .then())
                                    .thenReturn(s))
                            .switchIfEmpty(execute);
                })));
    }
}
