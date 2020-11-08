package insidebot;

import discord4j.rest.util.Color;

// юзается пока только ради цвета емебеда
// позже будет привязка к ивенту или нет
public enum AuditEventType{
    messageEdit(0x33CC33),
    messageClear(0xFF2400),
    messageDelete(0xFF2400),

    voiceJoin(0xFF33CC),
    voiceLeave(0x9900CC),

    userJoin(0x6666CC),
    userLeave(0x6633CC),
    userMute(0xFFCA59),
    userUnmute(0x85EA88),
    userBan(0xFFCC00);

    public final Color color;

    AuditEventType(int hex) {
        this.color = Color.of(hex);
    }
}
