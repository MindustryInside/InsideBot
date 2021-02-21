package inside.event.audit;

import discord4j.core.spec.EmbedCreateSpec;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import reactor.util.context.ContextView;

import static inside.util.ContextUtil.*;

public abstract class MessageAuditForwardProvider extends BaseAuditForwardProvider{

    public static final String KEY_MESSAGE_TXT = "message.txt";

    public static final String KEY_OLD_CONTENT = "old_content";

    public static final String KEY_MESSAGE_ID = "message_id";

    protected void addTimestamp(ContextView context, EmbedCreateSpec embed){
        embed.setFooter(DateTimeFormat.longDateTime().withLocale(context.get(KEY_LOCALE)).withZone(context.get(KEY_TIMEZONE)).print(DateTime.now()), null);
    }
}
