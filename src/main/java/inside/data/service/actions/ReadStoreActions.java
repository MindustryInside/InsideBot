package inside.data.service.actions;

import inside.data.entity.*;
import inside.data.service.api.EntityStoreAction;

public class ReadStoreActions{

    private ReadStoreActions(){}

    public static GetGuildConfigAction getGuildConfigById(long guildId){
        return new GetGuildConfigAction(guildId);
    }

    public static GetAdminConfigAction getAdminConfigById(long guildId){
        return new GetAdminConfigAction(guildId);
    }

    public static GetAuditConfigAction getAuditConfigById(long guildId){
        return new GetAuditConfigAction(guildId);
    }

    public static GetLocalMemberAction getLocalMemberById(long userId, long guildId){
        return new GetLocalMemberAction(userId, guildId);
    }

    public static GetAllLocalMembersAction getAllLocalMembers(){
        return new GetAllLocalMembersAction();
    }

    public static GetMessageInfoAction getMessageInfoById(long messageId){
        return new GetMessageInfoAction(messageId);
    }

    public static GetStarboardConfigAction getStarboardConfigById(long guildId){
        return new GetStarboardConfigAction(guildId);
    }

    public static GetStarboardAction getStarboardById(long guildId, long sourceMessageId){
        return new GetStarboardAction(guildId, sourceMessageId);
    }

    public static class GetGuildConfigAction implements EntityStoreAction<GuildConfig>{
        public final long guildId;

        private GetGuildConfigAction(long guildId){
            this.guildId = guildId;
        }
    }

    public static class GetAdminConfigAction implements EntityStoreAction<AdminConfig>{
        public final long guildId;

        private GetAdminConfigAction(long guildId){
            this.guildId = guildId;
        }
    }

    public static class GetAuditConfigAction implements EntityStoreAction<AuditConfig>{
        public final long guildId;

        private GetAuditConfigAction(long guildId){
            this.guildId = guildId;
        }
    }

    public static class GetLocalMemberAction implements EntityStoreAction<LocalMember>{
        public final long userId;
        public final long guildId;

        private GetLocalMemberAction(long userId, long guildId){
            this.userId = userId;
            this.guildId = guildId;
        }
    }

    public static class GetAllLocalMembersAction implements EntityStoreAction<LocalMember>{}

    public static class GetMessageInfoAction implements EntityStoreAction<MessageInfo>{
        public final long messageId;

        public GetMessageInfoAction(long messageId){
            this.messageId = messageId;
        }
    }

    public static class GetStarboardConfigAction implements EntityStoreAction<StarboardConfig>{
        public final long guildId;

        public GetStarboardConfigAction(long guildId){
            this.guildId = guildId;
        }
    }

    public static class GetStarboardAction implements EntityStoreAction<Starboard>{
        public final long guildId;
        public final long sourceMessageId;

        public GetStarboardAction(long guildId, long sourceMessageId){
            this.guildId = guildId;
            this.sourceMessageId = sourceMessageId;
        }
    }
}
