package inside.event.audit;

import discord4j.rest.util.Color;

public enum AuditActionType{
    MESSAGE_CREATE(0x1337),
    MESSAGE_EDIT(0x32cd32),
    MESSAGE_DELETE(0xff341c),
    MESSAGE_CLEAR(0xdc143c),

    REACTION_ADD(0xd2b48c),
    REACTION_REMOVE(0x8b4513),
    REACTION_REMOVE_ALL(0xb22222),

    VOICE_JOIN(0xff33cc),
    VOICE_LEAVE(0x9900cc),

    MEMBER_JOIN(0x6666cc),
    MEMBER_LEAVE(0x6633cc),
    MEMBER_KICK(0xffd37f),
    MEMBER_BAN(0xffcc00),
    MEMBER_MUTE(0xffca59),
    MEMBER_UNMUTE(0x85ea88);

    public final Color color;

    AuditActionType(int hex){
        this.color = Color.of(hex);
    }
}
