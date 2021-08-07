package inside;

import discord4j.rest.util.Color;
import inside.data.entity.AdminActionType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.*;
import java.util.*;

@ConfigurationProperties("insidebot")
public class Settings{

    private String token;

    private final Discord discord = new Discord();

    private final Defaults defaults = new Defaults();

    private final Audit audit = new Audit();

    private final Cache cache = new Cache();

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

    public Audit getAudit(){
        return audit;
    }

    public Cache getCache(){
        return cache;
    }

    public static class Discord{

        private int maxClearedCount = 100;

        private boolean encryptMessages = true;

        private boolean auditLogSaving = false;

        private Duration errorEmbedTtl = Duration.ofSeconds(7);

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

        public boolean isAuditLogSaving(){
            return auditLogSaving;
        }

        public void setAuditLogSaving(boolean auditLogSaving){
            this.auditLogSaving = auditLogSaving;
        }

        public Duration getErrorEmbedTtl(){
            return errorEmbedTtl;
        }

        public void setErrorEmbedTtl(Duration errorEmbedTtl){
            this.errorEmbedTtl = errorEmbedTtl;
        }
    }

    public static class Defaults{

        private List<String> prefixes = Arrays.asList("#");

        private ZoneId timeZone = ZoneId.of("Etc/Greenwich");

        private Locale locale = new Locale("en");

        private Color normalColor = Color.of(0xc4f5b7);

        private Color errorColor = Color.of(0xff3838);

        private int maxWarnings = 3;

        private Duration warnExpire = Duration.ofDays(21);

        private Duration muteEvade = Duration.ofDays(15);

        private AdminActionType thresholdAction = AdminActionType.ban;

        private int starboardLowerStarBarrier = 3;

        private Duration activeUserKeepCountingDuration = Duration.ofDays(21);

        private int activeUserMessageBarrier = 75;

        public List<String> getPrefixes(){
            return prefixes;
        }

        public void setPrefix(List<String> prefixes){
            this.prefixes = prefixes;
        }

        public ZoneId getTimeZone(){
            return timeZone;
        }

        public void setTimeZone(String timeZone){
            this.timeZone = ZoneId.of(timeZone);
        }

        public Locale getLocale(){
            return locale;
        }

        public void setLocale(Locale locale){
            this.locale = locale;
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

        public int getMaxWarnings(){
            return maxWarnings;
        }

        public void setMaxWarnings(int maxWarnings){
            this.maxWarnings = maxWarnings;
        }

        public Duration getWarnExpire(){
            return warnExpire;
        }

        public void setWarnExpire(Duration warnExpire){
            this.warnExpire = warnExpire;
        }

        public Duration getMuteEvade(){
            return muteEvade;
        }

        public void setMuteEvade(Duration muteEvade){
            this.muteEvade = muteEvade;
        }

        public AdminActionType getThresholdAction(){
            return thresholdAction;
        }

        public void setThresholdAction(AdminActionType thresholdAction){
            this.thresholdAction = thresholdAction;
        }

        public Duration getActiveUserKeepCountingDuration(){
            return activeUserKeepCountingDuration;
        }

        public void setActiveUserKeepCountingDuration(Duration activeUserKeepCountingDuration){
            this.activeUserKeepCountingDuration = activeUserKeepCountingDuration;
        }

        public int getActiveUserMessageBarrier(){
            return activeUserMessageBarrier;
        }

        public void setActiveUserMessageBarrier(int activeUserMessageBarrier){
            this.activeUserMessageBarrier = activeUserMessageBarrier;
        }

        public int getStarboardLowerStarBarrier(){
            return starboardLowerStarBarrier;
        }

        public void setStarboardLowerStarBarrier(int starboardLowerStarBarrier){
            this.starboardLowerStarBarrier = starboardLowerStarBarrier;
        }
    }

    public static class Audit{

        private Duration historyKeep = Duration.ofDays(31);

        private Duration memberKeep = Duration.ofDays(365);

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

    public static class Cache{

        private boolean activityConfig = true;

        private boolean adminConfig = true;

        private boolean auditConfig = true;

        private boolean emojiDispenser = true;

        private boolean guildConfig = true;

        private boolean localMember = true;

        private boolean starboard = true;

        private boolean starboardConfig = true;

        public boolean isActivityConfig(){
            return activityConfig;
        }

        public void setActivityConfig(boolean activityConfig){
            this.activityConfig = activityConfig;
        }

        public boolean isAdminConfig(){
            return adminConfig;
        }

        public void setAdminConfig(boolean adminConfig){
            this.adminConfig = adminConfig;
        }

        public boolean isAuditConfig(){
            return auditConfig;
        }

        public void setAuditConfig(boolean auditConfig){
            this.auditConfig = auditConfig;
        }

        public boolean isEmojiDispenser(){
            return emojiDispenser;
        }

        public void setEmojiDispenser(boolean emojiDispenser){
            this.emojiDispenser = emojiDispenser;
        }

        public boolean isGuildConfig(){
            return guildConfig;
        }

        public void setGuildConfig(boolean guildConfig){
            this.guildConfig = guildConfig;
        }

        public boolean isLocalMember(){
            return localMember;
        }

        public void setLocalMember(boolean localMember){
            this.localMember = localMember;
        }

        public boolean isStarboard(){
            return starboard;
        }

        public void setStarboard(boolean starboard){
            this.starboard = starboard;
        }

        public boolean isStarboardConfig(){
            return starboardConfig;
        }

        public void setStarboardConfig(boolean starboardConfig){
            this.starboardConfig = starboardConfig;
        }
    }
}
