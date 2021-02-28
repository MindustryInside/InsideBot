package inside;

import discord4j.rest.util.Color;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Settings{
    @Value("${spring.application.token}")
    public String token;

    public int historyExpireWeeks = 3;

    public int memberKeepMonths = 6;

    public String prefix = "$";

    public String timeZone = "Etc/Greenwich";

    public Color normalColor = Color.of(0xc4f5b7);

    public Color errorColor = Color.of(0xff3838);

    /* TODO: variable values */

    @Deprecated(forRemoval = true)
    public int maxWarnings = 3;

    @Deprecated(forRemoval = true)
    public int warnExpireDays = 20;

    @Deprecated(forRemoval = true)
    public int muteEvadeDays = 10;

    public int maxClearedCount = 100;
}
