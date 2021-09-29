package inside.command.common;

import discord4j.common.ReactorResources;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.util.Lazy;
import inside.util.codec.Base64Coder;
import inside.util.io.ReusableByteInputStream;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static inside.audit.BaseAuditProvider.MESSAGE_TXT;

@DiscordCommand(key = {"base64", "b64"}, params = "command.base64.params", description = "command.base64.description")
public class Base64Command extends Command{
    private final Lazy<HttpClient> httpClient = Lazy.of(ReactorResources.DEFAULT_HTTP_CLIENT);

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        boolean encode = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(str -> str.matches("^(?i)enc(ode)?$"))
                .orElse(false);

        AtomicBoolean attachmentMode = new AtomicBoolean(false);

        Mono<String> handleAttachment = Mono.justOrEmpty(env.message().getAttachments().stream()
                        .filter(att -> att.getWidth().isEmpty())
                        .filter(att -> att.getContentType().map(str -> str.startsWith("key")).orElse(true))
                        .findFirst())
                .flatMap(att -> httpClient.get().get().uri(att.getUrl())
                        .responseSingle((res, buf) -> buf.asString()))
                .doFirst(() -> attachmentMode.set(true));

        Mono<String> result = Mono.justOrEmpty(interaction.getOption(1)
                        .flatMap(CommandOption::getValue)
                        .map(OptionValue::asString))
                .switchIfEmpty(handleAttachment)
                .switchIfEmpty(messageService.err(env, "command.base64.missed-text").then(Mono.empty()))
                .map(String::trim)
                .flatMap(str -> Mono.fromCallable(() ->
                        encode ? Base64Coder.encodeString(str) : Base64Coder.decodeString(str)));

        return result.onErrorResume(IllegalArgumentException.class::isInstance,
                        t -> messageService.err(env, t.getMessage()).then(Mono.empty()))
                .flatMap(str -> Mono.defer(() -> {
                    var messageSpec = MessageCreateSpec.builder()
                            .messageReference(env.message().getId());

                    if(str.isBlank()){
                        messageSpec.content(messageService.get(env.context(), "message.placeholder"));
                    }else if(str.length() < Message.MAX_CONTENT_LENGTH && !attachmentMode.get()){
                        messageSpec.content(str);
                    }else if(str.length() > Message.MAX_CONTENT_LENGTH || attachmentMode.get()){
                        messageSpec.addFile(MESSAGE_TXT, ReusableByteInputStream.ofString(str));
                    }

                    return env.channel().createMessage(messageSpec.build());
                }))
                .then();
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.base64.help",
                        GuildConfig.formatPrefix(prefix))
                .then();
    }
}
