package inside.util;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResourceMessageSource {
    private static final ConcurrentMap<Tuple2<String, Locale>, MessageFormat> formats = new ConcurrentHashMap<>();

    private final String baseName;

    public ResourceMessageSource(String baseName) {
        this.baseName = Objects.requireNonNull(baseName, "baseName");
    }

    public String getBaseName() {
        return baseName;
    }

    public String get(String code, Locale locale) {
        return ResourceBundle.getBundle(baseName, locale).getString(code);
    }

    public String format(String code, Locale locale, Object... args) {
        if (args.length == 0) { // Чтобы не создавать лишних форматеров
            return get(code, locale);
        }

        var key = Tuples.of(code, locale);
        MessageFormat format = formats.computeIfAbsent(key, k -> {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
            return new MessageFormat(bundle.getString(code), locale);
        });

        return format.format(args);
    }
}
