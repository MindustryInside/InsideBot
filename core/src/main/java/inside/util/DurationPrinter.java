package inside.util;

import java.time.temporal.TemporalAmount;
import java.util.Locale;

interface DurationPrinter {

    int calculateFormattedLength(TemporalAmount temporalAmount, Locale locale);

    int countFieldsToFormat(TemporalAmount temporalAmount, int stopAt, Locale locale);

    void formatTo(StringBuilder buf, TemporalAmount temporalAmount, Locale locale);
}
