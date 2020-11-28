package insidebot.event.dispatcher;

import discord4j.core.object.entity.Guild;

public abstract class BaseEvent{
    protected Guild guild;

    public BaseEvent(Guild guild){
        this.guild = guild;
    }

    public Guild guild(){
        return guild;
    }
}
