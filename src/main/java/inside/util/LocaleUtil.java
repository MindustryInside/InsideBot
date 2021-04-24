package inside.util;

import reactor.util.annotation.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public abstract class LocaleUtil{

    private LocaleUtil(){}

    public static Map<String, Locale> locales;

    public static Map<String, Map<String, Pattern>> pluralRules;

    public static final String ruLocale = "ru";
    public static final String defaultLocale = "en";

    static{
        locales = Map.of(
                ruLocale, Locale.forLanguageTag(ruLocale),
                defaultLocale, Locale.forLanguageTag(defaultLocale)
        );

        pluralRules = Map.of(
                ruLocale, Map.of(
                        "zero", Pattern.compile("^\\d*0$"),
                        "one", Pattern.compile("^(-?\\d*[^1])?1$"),
                        "two", Pattern.compile("^(-?\\d*[^1])?2$"),
                        "few", Pattern.compile("(^(-?\\d*[^1])?3)|(^(-?\\d*[^1])?4)$"),
                        "many", Pattern.compile("^\\d+$")
                ),
                defaultLocale, Map.of(
                        "zero", Pattern.compile("^0$"),
                        "one", Pattern.compile("^1$"),
                        "other", Pattern.compile("^\\d+$")
                )
        );
    }

    @Nullable
    public static Locale get(String tag){
        return locales.get(tag);
    }

    public static String getCount(long value, Locale locale){
        String str = String.valueOf(value);
        Map<String, Pattern> rules = pluralRules.getOrDefault(locale.getLanguage(), pluralRules.get(defaultLocale));
        return rules.entrySet().stream()
                .filter(plural -> plural.getValue().matcher(str).find())
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("other");
    }

    public static Locale getDefaultLocale(){
        return Objects.requireNonNull(get(defaultLocale));
    }
}
