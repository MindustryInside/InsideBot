package inside;

import discord4j.rest.util.Color;
import org.joda.time.DateTimeZone;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("insidebot")
public class Settings{

    private String token;

    private final Discord discord = new Discord();

    private final Defaults defaults = new Defaults();

    private final Moderation moderation = new Moderation();

    private final Audit audit = new Audit();

    public String getToken(){
        return token;
    }

    public void setToken(String token){
        this.token = token;
    }

    public Discord getDiscord(){
        return discord;
    }

    public Defaults getDefaults(){
        return defaults;
    }

    public Moderation getModeration(){
        return moderation;
    }

    public Audit getAudit(){
        return audit;
    }

    public static class Discord{

        private int maxClearedCount = 100;

        private boolean encryptMessages = true;

        public int getMaxClearedCount(){
            return maxClearedCount;
        }

        public void setMaxClearedCount(int maxClearedCount){
            this.maxClearedCount = maxClearedCount;
        }

        public boolean isEncryptMessages(){
            return encryptMessages;
        }

        public void setEncryptMessages(boolean encryptMessages){
            this.encryptMessages = encryptMessages;
        }
    }

    public static class Defaults{

        private String prefix = "$";

        private DateTimeZone timeZone = DateTimeZone.forID("Etc/Greenwich");

        private Color normalColor = Color.of(0xc4f5b7);

        private Color errorColor = Color.of(0xff3838);

        public String getPrefix(){
            return prefix;
        }

        public void setPrefix(String prefix){
            this.prefix = prefix;
        }

        public DateTimeZone getTimeZone(){
            return timeZone;
        }

        public void setTimeZone(String timeZone){
            this.timeZone = DateTimeZone.forID(timeZone);
        }

        public Color getNormalColor(){
            return normalColor;
        }

        public void setNormalColor(int normalColor){
            this.normalColor = Color.of(normalColor);
        }

        public Color getErrorColor(){
            return errorColor;
        }

        public void setErrorColor(int errorColor){
            this.errorColor = Color.of(errorColor);
        }
    }

    public static class Moderation{

        /* TODO: variable values */

        private int maxWarnings = 3;

        private Duration warnExpire = Duration.ofDays(20);

        private Duration muteEvade = Duration.ofDays(10);

        @Deprecated(forRemoval = true)
        public int getMaxWarnings(){
            return maxWarnings;
        }

        public void setMaxWarnings(int maxWarnings){
            this.maxWarnings = maxWarnings;
        }

        @Deprecated(forRemoval = true)
        public Duration getWarnExpire(){
            return warnExpire;
        }

        public void setWarnExpire(Duration warnExpire){
            this.warnExpire = warnExpire;
        }

        @Deprecated(forRemoval = true)
        public Duration getMuteEvade(){
            return muteEvade;
        }

        public void setMuteEvade(Duration muteEvade){
            this.muteEvade = muteEvade;
        }
    }

    public static class Audit{

        private Duration historyKeep = Duration.ofDays(21);

        private Duration memberKeep = Duration.ofDays(160);

        public Duration getHistoryKeep(){
            return historyKeep;
        }

        public void setHistoryKeep(Duration historyKeep){
            this.historyKeep = historyKeep;
        }

        public Duration getMemberKeep(){
            return memberKeep;
        }

        public void setMemberKeep(Duration memberKeep){
            this.memberKeep = memberKeep;
        }
    }
}
