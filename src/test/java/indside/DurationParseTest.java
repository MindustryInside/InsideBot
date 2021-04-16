package indside;

import inside.util.MessageUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;

public class DurationParseTest{

    @Test
    public void parseUnsupported(){
        Assertions.assertNull(MessageUtil.parseDuration("1y"));
        Assertions.assertNull(MessageUtil.parseDuration("1mon"));
        Assertions.assertNull(MessageUtil.parseDuration("1w"));
        Assertions.assertNull(MessageUtil.parseDuration("1y1mon1w"));
    }

    @Test
    public void parseSingle(){
        Assertions.assertEquals(Duration.ofSeconds(1), MessageUtil.parseDuration("1s"));
        Assertions.assertEquals(Duration.ofMinutes(1), MessageUtil.parseDuration("1m"));
        Assertions.assertEquals(Duration.ofHours(1), MessageUtil.parseDuration("1h"));
        Assertions.assertEquals(Duration.ofDays(1), MessageUtil.parseDuration("1d"));
    }

    @Test
    public void parseMultiple(){
        Assertions.assertEquals(Duration.ofSeconds(13).plusMinutes(100), MessageUtil.parseDuration("100min13s"));
        Assertions.assertEquals(Duration.ofMinutes(76).plusDays(15), MessageUtil.parseDuration("15d76m"));
        Assertions.assertEquals(Duration.ofHours(Integer.MAX_VALUE).plusDays(Integer.MAX_VALUE), MessageUtil.parseDuration("2147483647d2147483647h"));

        Assertions.assertNotEquals(Duration.ofDays(Integer.MIN_VALUE), MessageUtil.parseDuration("-2147483648d"));
        Assertions.assertNotEquals(Duration.ofSeconds(10).plusMinutes(1)
                .plusHours(10).plusDays(100), MessageUtil.parseDuration("10s1min10h100d"));
    }

    @Test
    public void fallback(){
        Assertions.assertEquals(Duration.ofSeconds(13).plusMinutes(100), MessageUtil.parseDuration("PT1H40M13S"));
        Assertions.assertEquals(Duration.ofMinutes(76).plusDays(15), MessageUtil.parseDuration("PT361H16M"));
        Assertions.assertEquals(Duration.ofHours(Integer.MIN_VALUE).plusDays(Integer.MIN_VALUE), MessageUtil.parseDuration("PT-53687091200H"));
        Assertions.assertEquals(Duration.ofHours(Integer.MAX_VALUE), MessageUtil.parseDuration("PT2147483647H"));
    }
}
