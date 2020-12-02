package insidebot.util;

import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import discord4j.core.object.Region;

import java.util.*;

public class LocaleUtil{

    private LocaleUtil(){}

    public static ObjectMap<String, Locale> locales;

    public static final String ruLocale = "ru", enLocale = "en", defaultLocale = "en";

    static{
        locales = ObjectMap.of(
            ruLocale, Locale.forLanguageTag(ruLocale),
            enLocale, Locale.US,
            defaultLocale, Locale.ROOT
        );
    }

    public static Locale get(Region region){
        for(Entry<String, Locale> e : locales){
            if(e.key.equalsIgnoreCase(region.getName().substring(0, 2))) return e.value;
        }
        return getDefaultLocale();
    }

    public static Locale get(String tag) {
        return locales.get(tag);
    }

    public static Locale getOrDefault(String tag){
        return locales.get(tag, getDefaultLocale());
    }

    public static boolean isSupported(String tag){
        return locales.containsKey(tag);
    }

    public static Locale getDefaultLocale(){
        return get(defaultLocale);
    }
}
