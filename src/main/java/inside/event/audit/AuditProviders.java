package inside.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.spec.*;
import inside.data.entity.AuditAction;
import inside.data.entity.base.NamedReference;
import inside.util.MessageUtil;
import org.joda.time.format.*;
import reactor.util.context.ContextView;

import java.util.Objects;

import static inside.util.ContextUtil.*;

public class AuditProviders{

    private AuditProviders(){}

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

    @ForwardAuditProvider(AuditActionType.MESSAGE_CLEAR)
    public static class MessageClearAuditProvider extends MessageAuditProvider{
        public static final String KEY_COUNT = "count";

        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Integer count = action.getAttribute(KEY_COUNT);
            if(count == null){
                return;
            }

            embed.setTitle(messageService.format(context, "audit.message.clear.title", count, action.channel().name()));
            embed.setDescription(messageService.format(context, "audit.message.clear.description",
                                                       action.user().name(), count,
                                                       messageService.getCount(context, "common.plurals.message", count),
                                                       action.channel().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_JOIN)
    public static class UserJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setTitle(messageService.get(context, "audit.member.join.title"));
            embed.setDescription(messageService.format(context, "audit.member.join.description", action.user().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_LEAVE)
    public static class UserLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setTitle(messageService.get(context, "audit.member.leave.title"));
            embed.setDescription(messageService.format(context, "audit.member.leave.description", action.user().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_KICK)
    public static class UserKickAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String reason = action.getAttribute(KEY_REASON);
            NamedReference target = action.target();
            if(target == null || reason == null){
                return;
            }

            embed.setTitle(messageService.get(context, "audit.member.kick.title"));
            embed.setDescription(String.format("%s%n%s",
                    messageService.format(context, "audit.member.kick.description", target.name(), action.user().name()),
                    messageService.format(context, "common.reason", reason)
            ));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_BAN)
    public static class UserBanAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String reason = action.getAttribute(KEY_REASON);
            NamedReference target = action.target();
            if(reason == null){
                reason = messageService.get(context, "common.not-defined");
            }

            if(target == null){
                return;
            }

            embed.setTitle(messageService.get(context, "audit.member.ban.title"));
            embed.setDescription(String.format("%s%n%s",
                    messageService.format(context, "audit.member.ban.description", target.name(), action.user().name()),
                    messageService.format(context, "common.reason", reason)
            ));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_UNMUTE)
    public static class UserUnmuteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            NamedReference target = action.target();
            if(target == null){
                return;
            }

            embed.setTitle(messageService.get(context, "audit.member.unmute.title"));
            embed.setDescription(messageService.format(context, "audit.member.unmute.description", target.name()));
            // TODO(Skat): admin field
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_MUTE)
    public static class UserMuteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Long delay = action.getAttribute(KEY_DELAY);
            String reason = action.getAttribute(KEY_REASON);
            NamedReference target = action.target();
            if(reason == null){
                reason = messageService.get(context, "common.not-defined");
            }

            if(delay == null || target == null){
                return;
            }

            DateTimeFormatter formatter = DateTimeFormat.shortDateTime()
                    .withLocale(context.get(KEY_LOCALE))
                    .withZone(context.get(KEY_TIMEZONE));

            embed.setTitle(messageService.get(context, "audit.member.mute.title"));
            embed.setDescription(String.format("%s%n%s%n%s",
                    messageService.format(context, "audit.member.mute.description", target.name(), action.user().name()),
                    messageService.format(context, "common.reason", reason),
                    messageService.format(context, "audit.member.mute.delay", formatter.print(delay))
            ));
            // TODO(Skat): admin field
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
