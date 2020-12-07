package insidebot.common.command.service;

import arc.struct.Seq;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.*;
import insidebot.common.command.model.base.CommandReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommandHandler extends BaseCommandHandler{

    @Override
    public CommandResponse handleMessage(String message, CommandReference reference, MessageCreateEvent event){
        Member member = reference.member();
        Snowflake guildId = member.getGuildId();

        String prefix = guildService.prefix(guildId);

        if(event.getMessage().getUserMentions().map(User::getId).any(u -> u.equals(discordService.gateway().getSelfId())).blockOptional().orElse(false)){
            prefix = event.getClient().getSelf().map(User::getMention).blockOptional().orElse(prefix);
        }

        if(message == null || !message.startsWith(prefix)){
            return new CommandResponse(ResponseType.noCommand, null, null);
        }

        message = message.substring(prefix.length());

        String commandstr = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        String argstr = message.contains(" ") ? message.substring(commandstr.length() + 1) : "";

        Seq<String> result = new Seq<>();

        Command command = commands.get(commandstr);

        if(command != null){
            int index = 0;
            boolean satisfied = false;

            while(true){
                if(index >= command.params.length && !argstr.isEmpty()){
                    return new CommandResponse(ResponseType.manyArguments, command, commandstr);
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
                        return new CommandResponse(ResponseType.fewArguments, command, commandstr);
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
                return new CommandResponse(ResponseType.fewArguments, command, commandstr);
            }

            if(command.permissions != null  && !command.permissions.isEmpty() && member.getGuild().map(Objects::nonNull).blockOptional().orElse(false)){
                Mono<TextChannel> channel = event.getMessage().getChannel().cast(TextChannel.class);
                Member self = event.getGuild().flatMap(Guild::getSelfMember).blockOptional().orElseThrow(RuntimeException::new);
                PermissionSet selfPermissions = channel.flatMap(t -> t.getEffectivePermissions(self.getId()))
                                                       .blockOptional()
                                                       .orElse(PermissionSet.none());
                List<Permission> permissions = Flux.fromIterable(command.permissions)
                        .filterWhen(p -> channel.block().getEffectivePermissions(self.getId()).map(r -> !r.contains(p)))
                        .collectList()
                        .blockOptional()
                        .orElse(Collections.emptyList());

                if(!permissions.isEmpty()){
                    String bundled = permissions.stream().map(p -> messageService.getEnum(p)).collect(Collectors.joining("\n"));
                    if(selfPermissions.contains(Permission.SEND_MESSAGES)){
                        if(selfPermissions.contains(Permission.EMBED_LINKS)){
                            channel.flatMap(c -> c.createEmbed(e -> {
                                e.setColor(settings.normalColor);
                                e.setTitle(messageService.get("message.error.permission-denied.title"));
                                e.setDescription(messageService.format("message.error.permission-denied.description", bundled));
                            })).block();
                        }else{
                            String builder = String.format("%s%n%n%s",
                                                           messageService.get("message.error.permission-denied.title"),
                                                           messageService.format("message.error.permission-denied.description", bundled));
                            event.getMessage().getChannel().flatMap(c -> c.createMessage(builder)).block();
                        }
                        return new CommandResponse(ResponseType.permissionDenied, command, commandstr);
                    }
                }
            }

            command.runner.execute(reference, event, result.toArray(String.class));

            return new CommandResponse(ResponseType.valid, command, commandstr);
        }else{
            return new CommandResponse(ResponseType.unknownCommand, null, commandstr);
        }
    }
}
