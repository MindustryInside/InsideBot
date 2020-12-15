package inside.common.command.service;

import arc.util.Strings;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.*;
import inside.common.command.model.base.*;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandHandler extends BaseCommandHandler{

    @Override
    public Publisher<Void> handleMessage(String message, CommandReference reference){
        Mono<Guild> guild = reference.event().getGuild();
        Mono<TextChannel> channel = reference.getReplyChannel().cast(TextChannel.class);
        Member self = guild.flatMap(Guild::getSelfMember).blockOptional().orElseThrow(RuntimeException::new);

        String prefix = guildService.prefix(self.getGuildId());

        if(reference.event().getMessage().getUserMentions().map(User::getId).any(u -> u.equals(self.getId())).blockOptional().orElse(false)){
            prefix = self.getNicknameMention() + " ";
        }

        if(message == null || !message.startsWith(prefix)){
            return Mono.empty();
        }

        message = message.substring(prefix.length()).trim();

        String commandstr = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        String argstr = message.contains(" ") ? message.substring(commandstr.length() + 1) : "";

        LinkedList<String> result = new LinkedList<>();

        CommandInfo command = commands.get(commandstr).compile();

        if(command != null){
            int index = 0;
            boolean satisfied = false;

            while(true){
                if(index >= command.params.length && !argstr.isEmpty()){
                    return messageService.err(channel, messageService.get("command.response.many-arguments.title"),
                                       messageService.format("command.response.many-arguments.description",
                                                             prefix, command.text, command.paramText));
                }else if(argstr.isEmpty()) break;

                if(command.params[index].optional || index >= command.params.length - 1 || command.params[index + 1].optional){
                    satisfied = true;
                }

                if(command.params[index].variadic){
                    result.add(argstr);
                    break;
                }

                int next = argstr.indexOf(" ");
                if(next == -1){
                    if(!satisfied){
                        return messageService.err(channel, messageService.get("command.response.few-arguments.title"),
                                           messageService.format("command.response.few-arguments.description",
                                                                 prefix, command.text));
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

            if(!satisfied && command.params.length > 0 && !command.params[0].optional){
                return messageService.err(channel, messageService.get("command.response.few-arguments.title"),
                                          messageService.format("command.response.few-arguments.description",
                                                                prefix, command.text));
            }

            if(command.permissions != null  && !command.permissions.isEmpty()){
                PermissionSet selfPermissions = channel.flatMap(t -> t.getEffectivePermissions(self.getId()))
                                                       .blockOptional()
                                                       .orElse(PermissionSet.none());
                List<Permission> permissions = Flux.fromIterable(command.permissions)
                        .filterWhen(p -> channel.flatMap(c -> c.getEffectivePermissions(self.getId()).map(r -> !r.contains(p))))
                        .collectList()
                        .blockOptional()
                        .orElse(Collections.emptyList());

                if(!permissions.isEmpty()){
                    String bundled = permissions.stream().map(p -> messageService.getEnum(p)).collect(Collectors.joining("\n"));
                    if(selfPermissions.contains(Permission.SEND_MESSAGES)){
                        if(selfPermissions.contains(Permission.EMBED_LINKS)){
                            return messageService.info(
                                    channel,
                                    messageService.get("message.error.permission-denied.title"),
                                    messageService.format("message.error.permission-denied.description", bundled)
                            );
                        }
                        return messageService.text(channel, String.format("%s%n%n%s",
                                messageService.get("message.error.permission-denied.title"),
                                messageService.format("message.error.permission-denied.description", bundled)
                        ));
                    }
                }
            }

            return commands.get(command.text).execute(reference, result.toArray(new String[0]));
        }else{
            int min = 0;
            CommandInfo closest = null;

            for(CommandInfo cmd : commandList()){
                int dst = Strings.levenshtein(cmd.text, commandstr);
                if(dst < 3 && (closest == null || dst < min)){
                    min = dst;
                    closest = cmd;
                }
            }

            if(closest != null){
                return messageService.err(channel, messageService.format("command.response.found-closest", closest.text));
            }
            return messageService.err(channel, messageService.format("command.response.unknown", prefix));
        }
    }
}
