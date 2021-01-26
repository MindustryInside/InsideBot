package inside.event.dispatcher;

import discord4j.core.object.entity.Guild;

import java.util.Objects;

public abstract class BaseEvent{
    protected Guild guild;

    public BaseEvent(Guild guild){
        this.guild = Objects.requireNonNull(guild, "guild");
    }

    public Guild guild(){
        return guild;
    }
}
