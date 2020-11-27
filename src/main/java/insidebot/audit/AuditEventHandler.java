package insidebot.audit;

import discord4j.core.event.domain.Event;
import discord4j.core.spec.*;
import insidebot.event.EventHandler;
import insidebot.util.StringInputStream;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.function.Consumer;

public abstract class AuditEventHandler<T extends Event> implements EventHandler<T>{
    protected StringInputStream stringInputStream = new StringInputStream();

    public abstract Mono<Void> log(@NonNull MessageCreateSpec message);

    public Mono<Void> log(Consumer<EmbedCreateSpec> embed){
        return log(embed, false);
    }

    public Mono<Void> log(Consumer<EmbedCreateSpec> embed, boolean file){
        MessageCreateSpec m = new MessageCreateSpec().setEmbed(embed);
        return log(file ? m.addFile("message.txt", stringInputStream) : m);
    }

    //todo
}
