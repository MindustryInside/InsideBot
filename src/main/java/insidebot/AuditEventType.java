package insidebot;

import java.awt.*;

// юзается пока только ради цвета емебеда
public enum AuditEventType{
    messageEdit("#33CC33"),
    messageClear("#FF2400"),
    messageDelete("#FF2400"),

    voiceJoin("#FF33CC"),
    voiceLeave("#9900CC"),

    userJoin("#6666CC"),
    userLeave("#6633CC"),
    userMute("#FFCA59"),
    userUnmute("#85EA8A"),
    userBan("#FFCC00");

    public final Color color;

    AuditEventType() {
        this(null);
    }

    AuditEventType(String hex) {
        this.color = hex != null ? Color.decode(hex) : null;
    }
}
