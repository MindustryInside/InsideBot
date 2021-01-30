package inside.event.audit;

import discord4j.rest.util.Color;

public enum AuditEventType{
    messageEdit(0x32cd32),
    messageClear(0xdc143c),
    messageDelete(0xff341c),

    voiceJoin(0xff33cc),
    voiceLeave(0x9900cc),

    userJoin(0x6666cc),
    userLeave(0x6633cc),
    userKick(0xffd37f),
    userMute(0xffca59),
    userUnmute(0x85ea88),
    userBan(0xffcc00);

    public final Color color;

    AuditEventType(int hex){
        this.color = Color.of(hex);
    }
}
