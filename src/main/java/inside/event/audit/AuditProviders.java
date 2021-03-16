package inside.event.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.spec.*;
import inside.data.entity.AuditAction;
import inside.data.entity.base.NamedReference;
import inside.util.MessageUtil;
import org.joda.time.format.*;
import reactor.util.context.ContextView;

import static inside.event.audit.Attribute.*;
import static inside.util.ContextUtil.*;

public class AuditProviders{

    private AuditProviders(){}

    @ForwardAuditProvider(AuditActionType.MESSAGE_EDIT)
    public static class MessageEditAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Snowflake messageId = action.getAttribute(MESSAGE_ID);
            String oldContent = action.getAttribute(OLD_CONTENT);
            String newContent = action.getAttribute(NEW_CONTENT);
            String url = action.getAttribute(USER_URL);
            if(messageId == null || oldContent == null || newContent == null || url == null){
                return;
            }

            embed.setAuthor(action.user().name(), null, url);
            embed.setTitle(messageService.format(context, "audit.message.edit.title", action.channel().name()));
            embed.setDescription(messageService.format(context, "audit.message.edit.description",
                    action.guildId().asString(),
                    action.channel().id(),
                    messageId.asString()));

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
    public static class MessageDeleteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String oldContent = action.getAttribute(OLD_CONTENT);
            String url = action.getAttribute(USER_URL);
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
    public static class MessageClearAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Integer count = action.getAttribute(COUNT);
            if(count == null){
                return;
            }

            embed.setDescription(messageService.format(context, "audit.message.clear.description", count,
                    messageService.getCount(context, "common.plurals.message", count)));
            embed.addField(messageService.get(context, "audit.member.admin"), action.user().name(), true);
            embed.addField(messageService.get(context, "audit.message.channel"), action.channel().name(), true);
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_JOIN)
    public static class UserJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setDescription(messageService.format(context, "audit.member.join.description", action.user().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_LEAVE)
    public static class UserLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setDescription(messageService.format(context, "audit.member.leave.description", action.user().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_KICK)
    public static class UserKickAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String reason = action.getAttribute(REASON);
            NamedReference target = action.target();
            if(target == null || reason == null){
                return;
            }

            embed.setDescription(messageService.format(context, "audit.member.kick.title", target.name()));
            embed.addField(messageService.get(context, "audit.member.admin"), action.user().name(), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_BAN)
    public static class UserBanAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            String reason = action.getAttribute(REASON);
            NamedReference target = action.target();
            if(target == null || reason == null){
                return;
            }

            embed.setDescription(messageService.format(context, "audit.member.ban.title", target.name()));
            embed.addField(messageService.get(context, "audit.member.admin"), action.user().name(), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
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

            embed.setDescription(messageService.format(context, "audit.member.unmute.title", target.name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.USER_MUTE)
    public static class UserMuteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            Long delay = action.getAttribute(DELAY);
            String reason = action.getAttribute(REASON);
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

            embed.setDescription(messageService.format(context, "audit.member.mute.title", target.name()));
            embed.addField(messageService.get(context, "audit.member.admin"), action.user().name(), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
            embed.addField(messageService.get(context, "audit.member.mute.delay"), formatter.print(delay), true);
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_JOIN)
    public static class VoiceJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setDescription(messageService.format(context, "audit.voice.join.description",
                    action.user().name(), action.channel().name()));
            addTimestamp(context, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_LEAVE)
    public static class VoiceLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditAction action, ContextView context, MessageCreateSpec spec, EmbedCreateSpec embed){
            embed.setDescription(messageService.format(context, "audit.voice.leave.description",
                    action.user().name(), action.channel().name()));
            addTimestamp(context, embed);
        }
    }
}
