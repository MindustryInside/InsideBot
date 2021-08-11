package inside.resolver;

import discord4j.core.object.entity.Member;
import inside.service.MessageService;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.Supplier;

public class MemberPlaceholderResolver extends BasePlaceholderResolver{

    private final Member member;

    protected MemberPlaceholderResolver(ContextView context, MessageService messageService, Member member){
        super(context, messageService);
        this.member = Objects.requireNonNull(member, "member");
    }

    public static MemberPlaceholderResolver of(ContextView context, MessageService messageService, Member member){
        return new MemberPlaceholderResolver(context, messageService, member);
    }

    @Override
    protected Map<String, Supplier<?>> createAccessors(){
        return Map.of(
                "id", member.getId()::asString,
                "displayName", member::getDisplayName,
                "username", member::getUsername,
                "mention", member::getMention
        );
    }
}
