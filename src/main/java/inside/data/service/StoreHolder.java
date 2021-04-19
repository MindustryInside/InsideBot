package inside.data.service;

import inside.data.service.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StoreHolder{
    private final GuildConfigService guildConfigService;

    private final AdminConfigService adminConfigService;

    private final LocalMemberService localMemberService;

    private final AuditConfigService auditConfigService;

    private final MessageInfoService messageInfoService;

    public StoreHolder(@Autowired GuildConfigService guildConfigService,
                       @Autowired AdminConfigService adminConfigService,
                       @Autowired LocalMemberService localMemberService,
                       @Autowired AuditConfigService auditConfigService,
                       @Autowired MessageInfoService messageInfoService){
        this.guildConfigService = guildConfigService;
        this.adminConfigService = adminConfigService;
        this.localMemberService = localMemberService;
        this.auditConfigService = auditConfigService;
        this.messageInfoService = messageInfoService;
    }

    public GuildConfigService getGuildConfigService(){
        return guildConfigService;
    }

    public AdminConfigService getAdminConfigService(){
        return adminConfigService;
    }

    public LocalMemberService getLocalMemberService(){
        return localMemberService;
    }

    public AuditConfigService getAuditConfigService(){
        return auditConfigService;
    }

    public MessageInfoService getMessageInfoService(){
        return messageInfoService;
    }
}
