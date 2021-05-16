package inside.data.service.actions;

import inside.data.entity.*;
import inside.data.service.api.EntityStoreAction;

public class UpdateStoreActions{

    private UpdateStoreActions(){}

    public static GuildConfigSaveAction guildConfigSave(GuildConfig guildConfig){
        return new GuildConfigSaveAction(guildConfig);
    }

    public static AdminConfigSaveAction adminConfigSave(AdminConfig adminConfig){
        return new AdminConfigSaveAction(adminConfig);
    }

    public static AuditConfigSaveAction auditConfigSave(AuditConfig auditConfig){
        return new AuditConfigSaveAction(auditConfig);
    }

    public static LocalMemberSaveAction localMemberSave(LocalMember localMember){
        return new LocalMemberSaveAction(localMember);
    }

    public static MessageInfoSaveAction messageInfoSave(MessageInfo messageInfo){
        return new MessageInfoSaveAction(messageInfo);
    }

    public static MessageInfoDeleteAction messageInfoDelete(MessageInfo messageInfo){
        return new MessageInfoDeleteAction(messageInfo);
    }

    public static StarboardConfigSaveAction starboardConfigSave(StarboardConfig starboardConfig){
        return new StarboardConfigSaveAction(starboardConfig);
    }

    public static class GuildConfigSaveAction implements EntityStoreAction<Void>{
        public final GuildConfig guildConfig;

        public GuildConfigSaveAction(GuildConfig guildConfig){
            this.guildConfig = guildConfig;
        }
    }

    public static class AdminConfigSaveAction implements EntityStoreAction<Void>{
        public final AdminConfig adminConfig;

        public AdminConfigSaveAction(AdminConfig adminConfig){
            this.adminConfig = adminConfig;
        }
    }

    public static class AuditConfigSaveAction implements EntityStoreAction<Void>{
        public final AuditConfig auditConfig;

        public AuditConfigSaveAction(AuditConfig auditConfig){
            this.auditConfig = auditConfig;
        }
    }

    public static class LocalMemberSaveAction implements EntityStoreAction<Void>{
        public final LocalMember localMember;

        public LocalMemberSaveAction(LocalMember localMember){
            this.localMember = localMember;
        }
    }

    public static class MessageInfoSaveAction implements EntityStoreAction<Void>{
        public final MessageInfo messageInfo;

        public MessageInfoSaveAction(MessageInfo messageInfo){
            this.messageInfo = messageInfo;
        }
    }

    public static class MessageInfoDeleteAction implements EntityStoreAction<Void>{
        public final MessageInfo messageInfo;

        public MessageInfoDeleteAction(MessageInfo messageInfo){
            this.messageInfo = messageInfo;
        }
    }

    public static class StarboardConfigSaveAction implements EntityStoreAction<Void>{
        public final StarboardConfig starboardConfig;

        public StarboardConfigSaveAction(StarboardConfig starboardConfig){
            this.starboardConfig = starboardConfig;
        }
    }
}
