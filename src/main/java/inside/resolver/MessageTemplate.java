package inside.resolver;

import discord4j.core.object.entity.Member;
import inside.data.entity.WelcomeMessage;
import org.immutables.value.Value;

@Value.Immutable
public interface MessageTemplate{

    static ImmutableMessageTemplate.Builder builder(){
        return ImmutableMessageTemplate.builder();
    }

    WelcomeMessage template();

    Member member();
}
