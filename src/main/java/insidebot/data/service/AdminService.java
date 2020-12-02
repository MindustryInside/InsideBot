package insidebot.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import insidebot.data.entity.*;
import reactor.core.publisher.*;
import reactor.util.annotation.*;

import java.util.Calendar;

public interface AdminService{

    Flux<AdminAction> get(AdminActionType type, Snowflake guildId, Snowflake targetId);

    Mono<Void> mute(LocalMember admin, LocalMember target, Calendar end, @Nullable String reason);

    Mono<Void> unmute(Snowflake guildId, Snowflake targetId);

    Mono<Void> warn(LocalMember admin, LocalMember target, @Nullable String reason);

    Mono<Void> unwarn(Snowflake guildId, Snowflake targetId, int index);

    Flux<AdminAction> warnings(Snowflake guildId, Snowflake targetId);

    boolean isAdmin(Member member);

    boolean isOwner(Member member);

    enum AdminActionType{
        warn,
        mute,
        kick,
        ban
    }
}
