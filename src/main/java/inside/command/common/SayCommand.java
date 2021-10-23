package inside.command.common;

import discord4j.common.ReactorResources;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.MessageCreateFields;
import inside.command.Command;
import inside.command.model.*;
import inside.util.Lazy;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.util.stream.Collectors;

// for an interesting quotes
@DiscordCommand(key = "say", params = "command.say.params", description = "command.say.description")
public class SayCommand extends Command{

    private final Lazy<HttpClient> httpClient = Lazy.of(ReactorResources.DEFAULT_HTTP_CLIENT);

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption("text")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        var atts = env.message().getAttachments();
        if(!atts.isEmpty()){
            return messageService.text(env, text);
        }

        return Flux.fromIterable(atts)
                .flatMap(a -> httpClient.get().get().uri(a.getUrl())
                        .responseSingle((res, buf) -> buf.asInputStream()
                                .map(inputStream -> MessageCreateFields.File.of(
                                        a.getFilename(), inputStream))))
                .collectList()
                .flatMap(list -> messageService.text(env, text)
                        .withFiles(list));
    }
}
