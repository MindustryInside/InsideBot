package inside.util;

import discord4j.core.object.Region;
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

    public static Locale get(Region region){
        for(Map.Entry<String, Locale> entry : locales.entrySet()){
            if(entry.getKey().equalsIgnoreCase(region.getName().substring(0, 2))){
                return entry.getValue();
            }
        }
        return getDefaultLocale();
    }

    @Nullable
    public static Locale get(String tag){
        return locales.get(tag);
    }

    public static String getCount(long value, Locale locale){
        String str = String.valueOf(value);

        Map<String, Pattern> rules = pluralRules.getOrDefault(locale.getLanguage(), pluralRules.get(defaultLocale));
        for(Map.Entry<String, Pattern> plural : rules.entrySet()){
            if(plural.getValue().matcher(str).find()){
                return plural.getKey();
            }
        }

        return "other";
    }

    public static Locale getDefaultLocale(){
        return Objects.requireNonNull(get(defaultLocale));
    }
}
