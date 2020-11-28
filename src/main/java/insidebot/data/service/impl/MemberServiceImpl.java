package insidebot.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.LocalMember;
import insidebot.data.repository.LocalMemberRepository;
import insidebot.data.service.MemberService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.NonNull;

import java.util.function.Supplier;

@Service
public class MemberServiceImpl implements MemberService{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private Logger log;

    @Autowired
    private LocalMemberRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Member member){
        return get(member.getGuildId(), member.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Guild guild, User user){
        return get(guild.getId(), user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember get(Snowflake guildId, Snowflake userId){
        return repository.findByGuildIdAndId(guildId, userId);
    }

    @Override
    @Transactional
    public LocalMember getOr(Member member, Supplier<LocalMember> prov){
        return exists(member.getGuildId(), member.getId()) ? get(member) : prov.get();
    }

    @Override
    @Transactional
    public LocalMember getOr(Guild guild, User user, Supplier<LocalMember> prov){
        return exists(guild.getId(), user.getId()) ? get(guild, user) : prov.get();
    }

    @Override
    @Transactional
    public LocalMember getOr(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov){
        return exists(guildId, userId) ? get(guildId, userId) : prov.get();
    }

    @Override
    @Transactional
    public LocalMember save(LocalMember member){
        return repository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake guildId, Snowflake userId){
        return get(guildId, userId) != null;
    }

    @Scheduled(cron = "* */2 * * * *")
    public void unmuteUsers(){
        // Flux.fromIterable(repository.findAll()).filter(i -> i != null && isMuteEnd(i))
        //           .subscribe(l -> {
        //               discordService.eventListener().publish(new MemberUnmuteEvent(discordService.gateway().getGuildById(l.guildId()).block(), l));
        //           }, Log::err);
    }

    @Scheduled(cron = "0 * * * * *")
    public void activeUsers(){
        // Flux.fromIterable(repository.findAll()).filterWhen(u -> {
        //     return discordService.gateway().getMemberById(u.guildId(), u.id()).map(Objects::nonNull).filterWhen(b -> {
        //         return b ? Mono.just(true) : Mono.fromRunnable(() -> log.warn("User '{}' not found", u.effectiveName()));
        //     });
        // }).subscribe(u -> {
        //     Member member = discordService.gateway().getMemberById(u.guildId(), u.id()).block();
        //     if(isActiveUser(u)){
        //         member.addRole(activeUserRoleID).block();
        //     }else{
        //         member.removeRole(activeUserRoleID).block();
        //     }
        // }, Log::err);
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
