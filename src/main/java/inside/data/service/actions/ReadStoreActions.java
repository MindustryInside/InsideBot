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
}
