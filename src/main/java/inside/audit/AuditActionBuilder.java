package inside.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

public abstract class AuditActionBuilder{
    private final Instant timestamp;
    private final Snowflake guildId;
    private final AuditActionType type;
    private final Map<String, Object> attributes;
    private User user;
    @Nullable
    private User target;
    @Nullable
    private GuildChannel channel;
    @Nullable
    protected List<Tuple2<String, InputStream>> attachments;

    protected AuditActionBuilder(Snowflake guildId, AuditActionType type){
        this.guildId = Objects.requireNonNull(guildId, "guildId");
        this.type = Objects.requireNonNull(type, "type");
        this.timestamp = Instant.now();
        this.attributes = new HashMap<>(7);
    }

    public AuditActionBuilder withUser(User user){
        this.user = Objects.requireNonNull(user, "user");
        return this;
    }

    public AuditActionBuilder withTargetUser(@Nullable User user){
        this.target = user;
        return this;
    }

    public AuditActionBuilder withChannel(@Nullable GuildChannel channel){
        this.channel = channel;
        return this;
    }

    public <T> AuditActionBuilder withAttribute(Attribute<T> key, @Nullable T value){
        attributes.put(key.name, value);
        return this;
    }

    public AuditActionBuilder withAttachment(String key, InputStream data){
        if(attachments == null){
            attachments = new ArrayList<>(1);
        }
        attachments.add(Tuples.of(key, data));
        return this;
    }

    public Instant getTimestamp(){
        return timestamp;
    }

    public Snowflake getGuildId(){
        return guildId;
    }

    public AuditActionType getType(){
        return type;
    }

    public User getUser(){
        return user;
    }

    @Nullable
    public User getTarget(){
        return target;
    }

    @Nullable
    public GuildChannel getChannel(){
        return channel;
    }

    public Map<String, Object> getAttributes(){
        return attributes;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Attribute<T> key){
        Object value = attributes.get(key.name);
        return value != null ? (T)value : null;
    }

    public abstract Mono<Void> save();
}
