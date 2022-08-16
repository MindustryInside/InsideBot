package inside.util;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongToIntFunction;

public class ResourceMessageSource {
    public static final List<Locale> supportedLocaled = List.of(new Locale("ru"), Locale.ENGLISH);

    private static final Map<Locale, LongToIntFunction> pluralForms;

    static {
        // взято с вики KDE по локализации
        pluralForms = Map.of(
                supportedLocaled.get(0), n -> n == 1 ? 3 : n % 10 == 1 && n % 100 != 11
                        ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2,
                supportedLocaled.get(1), n -> n == 1 ? 0 : 1
        );
    }

    private final ConcurrentMap<Tuple2<String, Locale>, MessageFormat> formats = new ConcurrentHashMap<>();

    public final String baseName;

    public ResourceMessageSource(String baseName) {
        this.baseName = Objects.requireNonNull(baseName);
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

    public String plural(String key, Locale locale, long count) {
        int form = pluralForms.get(locale).applyAsInt(count);
        return get(key + '[' + form + ']', locale);
    }

    public boolean contains(String code, Locale locale) {
        return ResourceBundle.getBundle(baseName, locale).containsKey(code);
    }
}
