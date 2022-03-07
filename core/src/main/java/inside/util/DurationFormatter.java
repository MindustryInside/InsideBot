package inside.util;

import reactor.util.annotation.Nullable;

import java.io.IOException;
import java.time.temporal.TemporalAmount;
import java.util.Locale;
import java.util.Objects;

public class DurationFormatter {

    private final DurationPrinter printer;
    private final Locale locale;

    DurationFormatter(DurationPrinter printer) {
        this(printer, null);
    }

    DurationFormatter(DurationPrinter printer, @Nullable Locale locale) {
        this.printer = Objects.requireNonNull(printer, "printer");
        this.locale = locale;
    }

    public DurationPrinter getPrinter() {
        return printer;
    }

    public DurationFormatter withLocale(@Nullable Locale locale) {
        if (Objects.equals(locale, this.locale)) {
            return this;
        }
        return new DurationFormatter(printer, locale);
    }

    @Nullable
    public Locale getLocale() {
        return locale;
    }

    public void formatTo(Appendable appendable, TemporalAmount temporalAmount) {
        try {
            if (appendable instanceof StringBuilder buf) {
                printer.formatTo(buf, temporalAmount, locale);
            } else {
                // buffer output to avoid writing to appendable in case of error
                StringBuilder buf = new StringBuilder(printer.calculateFormattedLength(temporalAmount, locale));
                printer.formatTo(buf, temporalAmount, locale);
                appendable.append(buf);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    public String format(TemporalAmount temporalAmount) {
        StringBuilder buf = new StringBuilder(printer.calculateFormattedLength(temporalAmount, locale));
        formatTo(buf, temporalAmount);
        return buf.toString();
    }
}
