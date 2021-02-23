package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.util.Calendar;

public interface AdminService{

    Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId);

    Flux<AdminAction> getAll(AdminActionType type);

    Mono<Void> kick(LocalMember admin, LocalMember target, @Nullable String reason);

    Mono<Void> ban(LocalMember admin, LocalMember target, @Nullable String reason);

    Mono<Void> unban(Snowflake guildId, Snowflake targetId);

    Mono<Void> mute(LocalMember admin, LocalMember target, Calendar end, @Nullable String reason);

    Mono<Boolean> isMuted(Snowflake guildId, Snowflake targetId);

    Mono<Void> unmute(Member target);

    Mono<Void> warn(LocalMember admin, LocalMember target, @Nullable String reason);

    Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index);

    Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId);

    Mono<Boolean> isOwner(Member member);

    Mono<Boolean> isAdmin(Member member);

    void warningsMonitor();

    void mutesMonitor();

    enum AdminActionType{
        warn,
        mute,
        kick,
        ban
    }
}
