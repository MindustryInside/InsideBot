package inside.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.spec.*;
import inside.data.entity.AuditAction;
import inside.util.MessageUtil;
import reactor.util.context.ContextView;

public class AuditProviders{

    @ForwardAuditProvider(AuditActionType.MESSAGE_EDIT)
    public static class MessageEditAuditProvider extends MessageAuditProvider{
        public static final String KEY_NEW_CONTENT = "new_content";

        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Snowflake messageId = action.getAttribute(KEY_MESSAGE_ID);
            String oldContent = action.getAttribute(KEY_OLD_CONTENT);
            String newContent = action.getAttribute(KEY_NEW_CONTENT);
            String url = action.getAttribute(KEY_USER_URL);
            if(messageId == null || oldContent == null || newContent == null || url == null){
                return;
            }

            embed.setDescription(messageService.format(context, "audit.message.edit.description",
                                                       action.guildId().asString(),
                                                       action.channel().id(),
                                                       messageId.asString()));

            embed.setAuthor(action.user().name(), null, url);
            embed.setTitle(messageService.format(context, "audit.message.edit.title", action.channel().name()));

            if(oldContent.length() > 0){
                embed.addField(messageService.get(context, "audit.message.old-content.title"),
                               MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), false);
            }

            if(newContent.length() > 0){
                embed.addField(messageService.get(context, "audit.message.new-content.title"),
                               MessageUtil.substringTo(newContent, Embed.Field.MAX_VALUE_LENGTH), true);
            }

            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MESSAGE_DELETE)
    public static class MessageDeleteAuditProvider extends MessageAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String oldContent = action.getAttribute(KEY_OLD_CONTENT);
            String url = action.getAttribute(KEY_USER_URL);
            if(oldContent == null || url == null){
                return;
            }

            embed.setAuthor(action.user().name(), null, url);
            embed.setTitle(messageService.format(context, "audit.message.delete.title", action.channel().name()));

            if(oldContent.length() > 0){
                embed.addField(messageService.get(context, "audit.message.deleted-content.title"),
                               MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), true);
            }

            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_JOIN)
    public static class VoiceJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setTitle(messageService.get(context, "audit.voice.join.title"));
            embed.setDescription(messageService.format(context, "audit.voice.join.description", action.user().name(), action.channel().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_LEAVE)
    public static class VoiceLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setTitle(messageService.get(context, "audit.voice.leave.title"));
            embed.setDescription(messageService.format(context, "audit.voice.leave.description", action.user().name(), action.channel().name()));
            addTimestamp(context, embed);
        }
    }
}
