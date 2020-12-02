package insidebot.common.command.service;

import arc.struct.Seq;
import arc.util.Log;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.common.command.model.base.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommandHandler extends BaseCommandHandler{

    @Override
    public CommandResponse handleMessage(String message, CommandReference reference, MessageCreateEvent event){
        Snowflake guildId = event.getGuildId().orElse(null); //todo сделать поддержку лс команд
        if(guildId == null){
            return new CommandResponse(ResponseType.noCommand, null, null);
        }

        String prefix = guildService.prefix(guildId);

        if(message == null || (!message.startsWith(prefix))){
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

            command.runner.execute(reference, event, result.toArray(String.class));

            return new CommandResponse(ResponseType.valid, command, commandstr);
        }else{
            return new CommandResponse(ResponseType.unknownCommand, null, commandstr);
        }
    }
}
