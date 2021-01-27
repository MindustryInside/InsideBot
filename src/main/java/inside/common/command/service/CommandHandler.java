package inside.common.command.service;

import arc.util.Strings;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import inside.common.command.model.base.*;
import inside.util.MessageUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.util.*;

@Service
public class CommandHandler extends BaseCommandHandler{

    @Override
    public Mono<Void> handleMessage(String message, CommandReference ref){
        Mono<Guild> guild = ref.event().getGuild();
        Mono<TextChannel> channel = ref.getReplyChannel().cast(TextChannel.class);
        Member self = guild.flatMap(Guild::getSelfMember).blockOptional().orElseThrow(RuntimeException::new);

        String[] prefix = {entityRetriever.prefix(self.getGuildId())};

        if(ref.event().getMessage().getUserMentions().map(User::getId).any(u -> u.equals(self.getId())).blockOptional().orElse(false)){
            prefix[0] = self.getNicknameMention() + " "; //todo не очень нравится
        }

        if(MessageUtil.isEmpty(message) || !message.startsWith(prefix[0])){
            return Mono.empty();
        }

        message = message.substring(prefix[0].length()).trim();

        String commandstr = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        String argstr = message.contains(" ") ? message.substring(commandstr.length() + 1) : "";

        LinkedList<String> result = new LinkedList<>();

        Command cmd = commands.get(commandstr);

        if(cmd != null){
            CommandInfo command = cmd.compile();
            int index = 0;
            boolean satisfied = false;

            while(true){
                if(index >= command.params.length && !argstr.isEmpty()){
                    return messageService.err(channel, messageService.get(ref.context(), "command.response.many-arguments.title"),
                                       messageService.format(ref.context(), "command.response.many-arguments.description",
                                                             prefix[0], command.text, command.paramText));
                }else if(argstr.isEmpty()){
                    break;
                }

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
                        return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                                  messageService.format(ref.context(), "command.response.few-arguments.description",
                                                                        prefix[0], command.text));
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
                return messageService.err(channel, messageService.get(ref.context(), "command.response.few-arguments.title"),
                                          messageService.format(ref.context(), "command.response.few-arguments.description",
                                                                prefix[0], command.text));
            }

            //todo сделать полностью реактивно и с учётом контекста
            // if(command.permissions != null  && !command.permissions.isEmpty()){
            //     PermissionSet selfPermissions = channel.flatMap(t -> t.getEffectivePermissions(self.getId()))
            //                                            .blockOptional()
            //                                            .orElse(PermissionSet.none());
            //     List<Permission> permissions = Flux.fromIterable(command.permissions)
            //             .filterWhen(p -> channel.flatMap(c -> c.getEffectivePermissions(self.getId()).map(r -> !r.contains(p))))
            //             .collectList()
            //             .blockOptional()
            //             .orElse(Collections.emptyList());

            //     if(!permissions.isEmpty()){
            //         String bundled = permissions.stream().map(p -> messageService.getEnum(p)).collect(Collectors.joining("\n"));
            //         if(selfPermissions.contains(Permission.SEND_MESSAGES)){
            //             if(selfPermissions.contains(Permission.EMBED_LINKS)){
            //                 return messageService.info(
            //                         channel,
            //                         messageService.get("message.error.permission-denied.title"),
            //                         messageService.format("message.error.permission-denied.description", bundled)
            //                 );
            //             }
            //             return messageService.text(channel, String.format("%s%n%n%s",
            //                     messageService.get("message.error.permission-denied.title"),
            //                     messageService.format("message.error.permission-denied.description", bundled)
            //             ));
            //         }
            //     }
            // }

            return cmd.execute(ref, result.toArray(new String[0]));
        }else{
            int min = 0;
            CommandInfo closest = null;

            for(CommandInfo c : commandList()){
                int dst = Strings.levenshtein(c.text, commandstr);
                if(dst < 3 && (closest == null || dst < min)){
                    min = dst;
                    closest = c;
                }
            }


            if(closest != null){
                return messageService.err(channel, messageService.format(ref.context(), "command.response.found-closest", closest.text));
            }
            return messageService.err(channel, messageService.format(ref.context(), "command.response.unknown", prefix[0]));
        }
    }
}
