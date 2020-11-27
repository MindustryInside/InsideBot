package insidebot.data.service.impl;

import arc.Events;
import arc.util.Log;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import insidebot.EventType;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.*;
import insidebot.data.repository.LocalMemberRepository;
import insidebot.data.service.MemberService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Objects;
import java.util.function.Supplier;

import static insidebot.InsideBot.activeUserRoleID;

@Service
public class MemberServiceImpl implements MemberService{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private Logger log;

    @Autowired
    private LocalMemberRepository repository;

    @Override
    public LocalMember get(Member member){
        return get(member.getGuildId(), member.getId());
    }

    @Override
    public LocalMember get(Guild guild, User user){
        return get(guild.getId(), user.getId());
    }

    @Override
    public LocalMember get(Snowflake guildId, Snowflake userId){
        return repository.findByGuildIdAndUserId(guildId, userId);
    }

    @Override
    public LocalMember getOr(Member member, Supplier<LocalMember> prov){
        return get(member) != null ? get(member) : prov.get();
    }

    @Override
    public LocalMember getOr(Guild guild, User user, Supplier<LocalMember> prov){
        return get(guild, user) != null ? get(guild, user) : prov.get();
    }

    @Override
    public LocalMember getOr(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov){
        return get(guildId, userId) != null ? get(guildId, userId) : prov.get();
    }

    @Override
    public LocalMember save(LocalMember member){
        return repository.save(member);
    }

    @Override
    public boolean exists(Snowflake guildId, Snowflake userId){
        return get(guildId, userId) != null;
    }

    @Scheduled(cron = "*/2 * * * *")
    public void unmuteUsers(){
        repository.getAll().filter(i -> i != null && isMuteEnd(i))
                  .subscribe(info -> Events.fire(new EventType.MemberUnmuteEvent(info)), Log::err);
    }

    @Scheduled(cron = "* * * * *")
    public void activeUsers(){
        repository.getAll().filterWhen(u -> {
            return discordService.gateway().getMemberById(u.guildId(), u.id()).map(Objects::nonNull).filterWhen(b -> {
                return b ? Mono.just(true) : Mono.fromRunnable(() -> log.warn("User '{}' not found", u.effectiveName()));
            });
        }).subscribe(u -> {
            Member member = discordService.gateway().getMemberById(u.guildId(), u.id()).block(); // todo
            if(isActiveUser(u)){
                member.addRole(activeUserRoleID).block();
            }else{
                member.removeRole(activeUserRoleID).block();
            }
        }, Log::err);
    }

    @Override
    public boolean isAdmin(Member member){
        return member != null && member.getRoles().map(Role::getPermissions).any(r -> r.contains(Permission.ADMINISTRATOR))
                                       .blockOptional().orElse(false);
    }

    protected boolean isMuteEnd(@NonNull LocalMember member){
        return member.muteEndDate() != null && DateTime.now().isAfter(new DateTime(member.muteEndDate()));
    }

    protected boolean isActiveUser(@NonNull LocalMember member){
        if(member.lastSentMessage() == null) return false;
        DateTime last = new DateTime(member.lastSentMessage());
        int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

        if(diff >= 3){
            member.messageSeq(0);
            save(member);
            return false;
        }else return member.messageSeq() >= 75;
    }
}
