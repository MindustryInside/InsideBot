package inside.util;

import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import discord4j.core.object.Region;

import java.util.*;
import java.util.regex.Pattern;

public abstract class LocaleUtil{

    private LocaleUtil(){}

    public static ObjectMap<String, Locale> locales;

    public static ObjectMap<String, ObjectMap<String, Pattern>> pluralRules;

    public static final String ruLocale = "ru", enLocale = "en", defaultLocale = "en";

    static{
        locales = ObjectMap.of(
                ruLocale, Locale.forLanguageTag(ruLocale),
                enLocale, Locale.forLanguageTag(enLocale),
                defaultLocale, Locale.forLanguageTag(defaultLocale)
        );

        pluralRules = ObjectMap.of(
                ruLocale, ObjectMap.of(
                        "zero", Pattern.compile("^\\d*0$"),
                        "one", Pattern.compile("^(-?\\d*[^1])?1$"),
                        "two", Pattern.compile("^(-?\\d*[^1])?2$"),
                        "few", Pattern.compile("(^(-?\\d*[^1])?3)|(^(-?\\d*[^1])?4)$"),
                        "many", Pattern.compile("^\\d+$")
                ),
                enLocale, ObjectMap.of(
                        "zero", Pattern.compile("^0$"),
                        "one", Pattern.compile("^1$"),
                        "other", Pattern.compile("^\\d+$")
                )
        );
    }

    public static Locale get(Region region){
        for(Entry<String, Locale> entry : locales){
            if(entry.key.equalsIgnoreCase(region.getName().substring(0, 2))){
                return entry.value;
            }
        }
        return getDefaultLocale();
    }

    public static Locale get(String tag){
        return locales.get(tag);
    }

    public static String getCount(long value, Locale locale){
        String str = String.valueOf(value);
        String key = null;

        ObjectMap<String, Pattern> rules = pluralRules.get(locale.getLanguage());
        if(rules == null){
            rules = pluralRules.get(defaultLocale);
        }

        for(Entry<String, Pattern> plural : rules){
            if(plural.value.matcher(str).find()){
                key = plural.key;
                break;
            }
        }

        return key != null ? key : "other";
    }

    public static Locale getOrDefault(String tag){
        return locales.get(tag, getDefaultLocale());
    }

    public static Locale getDefaultLocale(){
        return get(defaultLocale);
    }
}
