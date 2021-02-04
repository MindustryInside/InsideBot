package inside.common.command.service;

import arc.util.Strings;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import inside.common.command.model.base.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

import java.util.LinkedList;
import java.util.stream.Collectors;

@Service
public class CommandHandler extends BaseCommandHandler{

    @Override
    public Mono<Void> handleMessage(final String message, final CommandReference ref){
        Mono<Guild> guild = ref.event().getGuild();
        Mono<TextChannel> channel = ref.getReplyChannel().ofType(TextChannel.class);
        Mono<User> self = ref.getClient().getSelf();

        final String prefix = entityRetriever.prefix(ref.getAuthorAsMember().getGuildId());

        Mono<Tuple2<String, String>> text = Mono.justOrEmpty(prefix).filter(message::startsWith)
                .map(s -> message.substring(s.length()).trim())
                .zipWhen(s -> Mono.justOrEmpty(s.contains(" ") ? s.substring(0, s.indexOf(" ")) : s));

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
        });

        return text.flatMap(t -> Mono.defer(() -> commands.containsKey(t.getT2()) ? Mono.just(commands.get(t.getT2())) : suggestion)
                .ofType(Command.class)
                .flatMap(command -> {
                    CommandInfo commandInfo = command.compile();
                    LinkedList<String> result = new LinkedList<>();
                    String argstr = t.getT1().contains(" ") ? t.getT1().substring(t.getT2().length() + 1) : "";
                    int index = 0;
                    boolean satisfied = false;

                    while(true){
                        if(index >= commandInfo.params.length && !argstr.isEmpty()){
                            return messageService.err(channel, messageService.get(ref.context(), "command.response.many-arguments.title"),
                                                      messageService.format(ref.context(), "command.response.many-arguments.description",
                                                                            prefix, commandInfo.text, commandInfo.paramText));
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
                                return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                                          messageService.format(ref.context(), "command.response.few-arguments.description",
                                                                                prefix, commandInfo.text));
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
                        return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                                  messageService.format(ref.context(), "command.response.few-arguments.description",
                                                                        prefix, commandInfo.text));
                    }

                    return Flux.fromIterable(commandInfo.permissions)
                            .filterWhen(permission -> channel.zipWith(self.map(User::getId))
                                    .flatMap(TupleUtils.function((targetChannel, selfId) -> targetChannel.getEffectivePermissions(selfId)))
                                    .map(set -> !set.contains(permission)))
                            .map(permission -> messageService.getEnum(ref.context(), permission))
                            .collect(Collectors.joining("\n"))
                            .flatMap(s -> s.isEmpty() ? command.execute(ref, result.toArray(new String[0])) : messageService.text(channel, String.format("%s%n%n%s",
                                    messageService.get(ref.context(), "message.error.permission-denied.title"),
                                    messageService.format(ref.context(), "message.error.permission-denied.description", s)))
                                    .onErrorResume(__ -> guild.flatMap(g -> g.getOwner().flatMap(User::getPrivateChannel))
                                            .flatMap(c -> c.createMessage(String.format("%s%n%n%s",
                                            messageService.get(ref.context(), "message.error.permission-denied.title"),
                                            messageService.format(ref.context(), "message.error.permission-denied.description", s))))
                                            .then()));
                }));
    }
}
