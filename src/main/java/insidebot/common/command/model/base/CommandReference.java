package insidebot.common.command.model.base;

import discord4j.core.object.entity.*;
import insidebot.data.entity.*;

/**
 * Дата-класс с информацией о пользователе
 */
public class CommandReference{
    private Member member;
    private LocalMember localMember;
    private User user;
    private LocalUser localUser;

    public Member member(){
        return member;
    }

    public CommandReference member(Member member){
        this.member = member;
        return this;
    }

    public LocalMember localMember(){
        return localMember;
    }

    public CommandReference localMember(LocalMember localMember){
        this.localMember = localMember;
        return this;
    }

    public User user(){
        return user;
    }

    public CommandReference user(User user){
        this.user = user;
        return this;
    }

    public LocalUser localUser(){
        return localUser;
    }

    public CommandReference localUser(LocalUser localUser){
        this.localUser = localUser;
        return this;
    }
}
