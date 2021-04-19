package inside.event.audit;

import discord4j.rest.util.Color;

public enum AuditActionType{
    MESSAGE_CREATE(0x1337),
    MESSAGE_EDIT(0x32cd32),
    MESSAGE_DELETE(0xff341c),
    MESSAGE_CLEAR(0xdc143c),

    VOICE_JOIN(0xff33cc),
    VOICE_LEAVE(0x9900cc),

    USER_JOIN(0x6666cc),
    USER_LEAVE(0x6633cc),
    USER_KICK(0xffd37f),
    USER_BAN(0xffcc00),
    USER_MUTE(0xffca59),
    USER_UNMUTE(0x85ea88);

    public final Color color;

    AuditActionType(int hex){
        this.color = Color.of(hex);
    }
}
