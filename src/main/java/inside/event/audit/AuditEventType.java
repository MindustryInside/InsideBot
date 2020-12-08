package inside.event.audit;

import discord4j.rest.util.Color;

public enum AuditEventType{
    messageEdit(0x33CC33),
    messageClear(0xFF2400),
    messageDelete(0xFF2400),

    voiceJoin(0xFF33CC),
    voiceLeave(0x9900CC),

    userJoin(0x6666CC),
    userLeave(0x6633CC),
    userKick(0xFFD37F),
    userMute(0xFFCA59),
    userUnmute(0x85EA88),
    userBan(0xFFCC00);

    public final Color color;

    AuditEventType(int hex){
        this.color = Color.of(hex);
    }
}
