package insidebot.event;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ReadyHandler implements EventHandler<ReadyEvent>{
    @Autowired
    private Logger log;

    @Override
    public Class<ReadyEvent> type(){
        return ReadyEvent.class;
    }

    @Override
    public Mono<Void> onEvent(ReadyEvent event){
        return Mono.fromRunnable(() -> log.info("Bot up.")).then();
    }
}
