package inside.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildChannel;
import inside.data.entity.*;
import inside.data.entity.base.NamedReference;
import org.joda.time.DateTime;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.*;

import java.io.InputStream;
import java.util.*;

public abstract class AuditActionBuilder{

    protected final AuditAction action;

    protected List<Tuple2<String, InputStream>> attachments;

    protected AuditActionBuilder(Snowflake guildId, AuditActionType type){
        action = new AuditAction(guildId);
        action.timestamp(DateTime.now());
        action.type(type);
        action.attributes(new HashMap<>(7));
    }

    public AuditActionBuilder withUser(Member user){
        action.user(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(User user){
        action.user(getReference(user));
        return this;
    }

    public AuditActionBuilder withUser(LocalMember user){
        action.user(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(Member user){
        action.target(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(User user){
        action.target(getReference(user));
        return this;
    }

    public AuditActionBuilder withTargetUser(LocalMember user){
        action.target(getReference(user));
        return this;
    }

    public AuditActionBuilder withChannel(GuildChannel channel){
        action.channel(getReference(channel));
        return this;
    }

    public <T> AuditActionBuilder withAttribute(Attribute<T> key, @Nullable T value){
        action.attributes().put(key.name, value);
        return this;
    }

    public AuditActionBuilder withAttachment(String key, InputStream data){
        if(attachments == null){
            attachments = new ArrayList<>(1);
        }
        attachments.add(Tuples.of(key, data));
        return this;
    }

    private NamedReference getReference(Member member){
        return new NamedReference(member.getId(), member.getUsername());
    }

    private NamedReference getReference(User user){
        return new NamedReference(user.getId(), user.getUsername());
    }

    private NamedReference getReference(LocalMember member){
        return new NamedReference(member.userId(), member.effectiveName());
    }

    private NamedReference getReference(GuildChannel channel){
        return new NamedReference(channel.getId(), channel.getName());
    }

    public abstract Mono<Void> save();
}
