package inside.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import inside.data.entity.base.NamedReference;
import inside.util.*;
import reactor.util.context.ContextView;

import java.time.Instant;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;

import static inside.audit.Attribute.*;
import static inside.util.ContextUtil.*;

public class AuditProviders{

    private AuditProviders(){
    }

    //region message

    @ForwardAuditProvider(AuditActionType.MESSAGE_EDIT)
    public static class MessageEditAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Snowflake messageId = action.getAttribute(MESSAGE_ID);
            String oldContent = action.getAttribute(OLD_CONTENT);
            String newContent = action.getAttribute(NEW_CONTENT);
            String url = action.getAttribute(AVATAR_URL);
            Message message = action.getAttribute(MESSAGE);
            if(messageId == null || oldContent == null || newContent == null || url == null || message == null){
                return;
            }

            String guildIdString = action.getGuildId().asString();
            String channelIdString = message.getChannelId().asString();

            embed.author(formatName(action.getUser()), null, url);
            embed.description(messageService.format(context, "audit.message.edit.description",
                    guildIdString, channelIdString, messageId.asString()));

            if(!oldContent.isEmpty()){
                embed.addField(messageService.get(context, "audit.message.old-content.title"),
                        MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), false);
            }

            if(!newContent.isEmpty()){
                embed.addField(messageService.get(context, "audit.message.new-content.title"),
                        MessageUtil.substringTo(newContent, Embed.Field.MAX_VALUE_LENGTH), true);
            }

            message.getMessageReference()
                    .flatMap(MessageReference::getMessageId)
                    .map(Snowflake::asString)
                    .ifPresent(messageIdString -> embed.addField(
                            messageService.get(context, "audit.message.referenced.title"),
                            messageService.format(context, "audit.message.edit.description",
                                    guildIdString, channelIdString, messageIdString), false));

            embed.addField(messageService.get(context, "audit.message.channel"),
                    getReferenceContent(context, action.getChannel()), false);

            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MESSAGE_DELETE)
    public static class MessageDeleteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String oldContent = action.getAttribute(OLD_CONTENT);
            String url = action.getAttribute(AVATAR_URL);
            User target = action.getTarget();
            Message message = action.getAttribute(MESSAGE);
            if(oldContent == null || url == null || target == null || message == null){
                return;
            }

            String guildIdString = action.getGuildId().asString();
            String channelIdString = message.getChannelId().asString();

            embed.author(formatName(target), null, url);

            if(!oldContent.isEmpty()){
                embed.addField(messageService.get(context, "audit.message.deleted-content.title"),
                        MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), true);
            }

            message.getMessageReference()
                    .flatMap(MessageReference::getMessageId)
                    .map(Snowflake::asString)
                    .ifPresent(messageIdString -> embed.addField(
                            messageService.get(context, "audit.message.referenced.title"),
                            messageService.format(context, "audit.message.edit.description",
                                    guildIdString, channelIdString, messageIdString), false));

            embed.addField(messageService.get(context, "audit.message.channel"),
                    getReferenceContent(context, action.getChannel()), false);

            if(!action.getUser().equals(target)){
                embed.addField(messageService.get(context, "audit.message.responsible-user"),
                        getReferenceContent(context, action.getUser()), false);
            }

            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MESSAGE_CLEAR)
    public static class MessageClearAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Long count = action.getAttribute(COUNT);
            if(count == null){
                return;
            }

            embed.description(messageService.format(context, "audit.message.clear.description", count,
                    messageService.getPluralized(context, "common.plurals.message", count)));
            embed.addField(messageService.get(context, "audit.member.admin"),
                    getReferenceContent(context, action.getUser()), true);
            embed.addField(messageService.get(context, "audit.message.channel"),
                    getReferenceContent(context, action.getChannel()), false);
            addTimestamp(context, action, embed);
        }
    }

    //endregion
    //region moderation

    @ForwardAuditProvider(AuditActionType.MEMBER_JOIN)
    public static class MemberJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            embed.description(messageService.format(context, "audit.member.join.description",
                    getReferenceContent(context, action.getUser())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_LEAVE)
    public static class MemberLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            embed.description(messageService.format(context, "audit.member.leave.description",
                    getReferenceContent(context, action.getUser())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_KICK)
    public static class MemberKickAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String reason = action.getAttribute(REASON);
            User target = action.getTarget();
            if(target == null || reason == null){
                return;
            }

            embed.description(messageService.format(context, "audit.member.kick.title", getReferenceContent(context, target)));
            embed.addField(messageService.get(context, "audit.member.admin"),
                    getReferenceContent(context, action.getUser()), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_BAN)
    public static class MemberBanAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String reason = action.getAttribute(REASON);
            User target = action.getTarget();
            if(target == null || reason == null){
                return;
            }

            embed.description(messageService.format(context, "audit.member.ban.title", getReferenceContent(context, target)));
            embed.addField(messageService.get(context, "audit.member.admin"),
                    getReferenceContent(context, action.getUser()), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_UNMUTE)
    public static class MemberUnmuteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            User target = action.getTarget();
            if(target == null){
                return;
            }

            embed.description(messageService.format(context, "audit.member.unmute.title", getReferenceContent(context, target)));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_MUTE)
    public static class MemberMuteAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Instant delay = action.getAttribute(DELAY);
            String reason = action.getAttribute(REASON);
            User target = action.getTarget();
            if(reason == null){
                reason = messageService.get(context, "common.not-defined");
            }

            if(delay == null || target == null){
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(context.get(KEY_LOCALE))
                    .withZone(context.get(KEY_TIMEZONE));

            embed.description(messageService.format(context, "audit.member.mute.title", getReferenceContent(context, target)));
            embed.addField(messageService.get(context, "audit.member.admin"),
                    getReferenceContent(context, action.getUser()), true);
            embed.addField(messageService.get(context, "audit.member.reason"), reason, true);
            embed.addField(messageService.get(context, "audit.member.mute.delay"),
                    formatter.format(delay), true);
            addTimestamp(context, action, embed);
        }
    }

    //endregion
    //region member

    @ForwardAuditProvider(AuditActionType.MEMBER_ROLE_ADD)
    public static class MemberRoleAddAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String url = action.getAttribute(AVATAR_URL);
            Collection<Snowflake> roleIds = action.getAttribute(ROLE_IDS);
            if(url == null || roleIds == null){
                return;
            }

            embed.author(formatName(action.getUser()), null, url);
            embed.description(messageService.format(context, "audit.member.role-add.title",
                    messageService.getPluralized(context, "audit.plurals.role", roleIds.size()),
                    roleIds.stream().map(DiscordUtil::getRoleMention).collect(Collectors.joining(", ")),
                    getReferenceContent(context, action.getUser())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_ROLE_REMOVE)
    public static class MemberRoleRemoveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String url = action.getAttribute(AVATAR_URL);
            Collection<Snowflake> roleIds = action.getAttribute(ROLE_IDS);
            if(url == null || roleIds == null){
                return;
            }

            embed.author(formatName(action.getUser()), null, url);
            embed.description(messageService.format(context, "audit.member.role-remove.title",
                    messageService.getPluralized(context, "audit.plurals.role", roleIds.size()),
                    roleIds.stream().map(DiscordUtil::getRoleMention).collect(Collectors.joining(", ")),
                    getReferenceContent(context, action.getUser())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_AVATAR_UPDATE)
    public static class MemberAvatarUpdateAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String url = action.getAttribute(AVATAR_URL);
            String oldUrl = action.getAttribute(OLD_AVATAR_URL);
            if(oldUrl == null || url == null){
                return;
            }

            embed.author(formatName(action.getUser()), null, url);
            embed.description(messageService.format(context, "audit.member.avatar-update.title",
                    getReferenceContent(context, action.getUser())));
            embed.thumbnail(oldUrl);
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.MEMBER_NICKNAME_UPDATE)
    public static class MemberNicknameUpdate extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            String url = action.getAttribute(AVATAR_URL);
            String oldNickname = action.getAttribute(OLD_NICKNAME);
            String newNickname = action.getAttribute(NEW_NICKNAME);
            if(url == null || oldNickname == null || newNickname == null){
                return;
            }

            embed.author(formatName(action.getUser()), null, url);
            embed.description(messageService.format(context, "audit.member.nickname-update.title",
                    getReferenceContent(context, action.getUser()),
                    oldNickname, newNickname));
            addTimestamp(context, action, embed);
        }
    }

    //endregion
    //region reaction

    @ForwardAuditProvider(AuditActionType.REACTION_ADD)
    public static class ReactionAddAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Snowflake messageId = action.getAttribute(MESSAGE_ID);
            ReactionEmoji emoji = action.getAttribute(REACTION_EMOJI);
            if(messageId == null || emoji == null){
                return;
            }

            embed.description(messageService.format(context, "audit.reaction.add.description",
                    getReferenceContent(context, action.getUser()), DiscordUtil.getEmojiString(emoji),
                    action.getGuildId().asString(), action.getChannel().getId().asString(), messageId.asString()));

            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.REACTION_REMOVE)
    public static class ReactionRemoveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Snowflake messageId = action.getAttribute(MESSAGE_ID);
            ReactionEmoji emoji = action.getAttribute(REACTION_EMOJI);
            if(messageId == null || emoji == null){
                return;
            }

            embed.description(messageService.format(context, "audit.reaction.remove.description",
                    DiscordUtil.getEmojiString(emoji), getReferenceContent(context, action.getUser()),
                    action.getGuildId().asString(), action.getChannel().getId().asString(), messageId.asString()));

            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.REACTION_REMOVE_ALL)
    public static class ReactionRemoveAllAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            Snowflake messageId = action.getAttribute(MESSAGE_ID);
            if(messageId == null){
                return;
            }

            embed.description(messageService.format(context, "audit.reaction.remove-all.description",
                    action.getGuildId().asString(), action.getChannel().getId().asString(), messageId.asString()));

            addTimestamp(context, action, embed);
        }
    }

    //endregion
    //region voice

    @ForwardAuditProvider(AuditActionType.VOICE_JOIN)
    public static class VoiceJoinAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            embed.description(messageService.format(context, "audit.voice.join.description",
                    getReferenceContent(context, action.getUser()), getShortReference(context, action.getChannel())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_LEAVE)
    public static class VoiceLeaveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            embed.description(messageService.format(context, "audit.voice.leave.description",
                    getReferenceContent(context, action.getUser()), getShortReference(context, action.getChannel())));
            addTimestamp(context, action, embed);
        }
    }

    @ForwardAuditProvider(AuditActionType.VOICE_MOVE)
    public static class VoiceMoveAuditProvider extends BaseAuditProvider{
        @Override
        protected void build(AuditActionBuilder action, ContextView context, EmbedCreateSpec.Builder embed){
            VoiceChannel oldChannel = action.getAttribute(OLD_CHANNEL);
            if(oldChannel == null){
                return;
            }

            embed.description(messageService.format(context, "audit.voice.move.description",
                    getReferenceContent(context, action.getUser()),
                    getShortReference(context, oldChannel),
                    getShortReference(context, action.getChannel())));
            addTimestamp(context, action, embed);
        }
    }

    //endregion
}
