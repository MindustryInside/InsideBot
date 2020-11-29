package insidebot.common.services.impl;

import discord4j.common.util.Snowflake;
import discord4j.rest.util.Color;
import insidebot.Settings;
import insidebot.data.entity.GuildConfig;
import insidebot.data.service.GuildService;
import insidebot.common.services.ContextService;
import insidebot.util.LocaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ContextServiceImpl implements ContextService{

    private final ThreadLocal<Locale> localeHolder = new NamedThreadLocal<>("ContextServiceImpl.Locale");

    private final ThreadLocal<Color> colorHolder = new NamedThreadLocal<>("ContextServiceImpl.Color");

    private final ThreadLocal<Snowflake> guildHolder = new NamedThreadLocal<>("ContextServiceImpl.GuildIds");

    @Autowired
    private GuildService guildService;

    @Autowired
    private Settings settings;

    @Override
    public void init(Snowflake guildId){
        GuildConfig g = guildService.get(guildId);
        locale(locale(g.id()));
    }

    @Override
    public Locale locale(){
        Locale locale = localeHolder.get();
        if(locale == null){
            Snowflake guildId = guildHolder.get();
            if(guildId != null){
                locale(guildService.locale(guildId));
                locale = localeHolder.get();
            }
        }
        return locale != null ? locale : LocaleUtil.getDefaultLocale();
    }

    @Override
    public void locale(Locale locale){
        if(locale == null){
            localeHolder.remove();
        }else{
            localeHolder.set(locale);
        }
    }

    @Override
    public Color color(){ //todo
        return settings.normalColor;
    }

    @Override
    public void color(Color color){
        if(color == null){
            colorHolder.remove();
        }else{
            colorHolder.set(color);
        }
    }

    @Override
    public Locale locale(String locale){
        return LocaleUtil.getOrDefault(locale);
    }

    @Override
    public Locale locale(Snowflake guildId){
        return LocaleUtil.getOrDefault(guildService.locale(guildId));
    }

    @Override
    public void reset(){
        guildHolder.remove();
        localeHolder.remove();
        colorHolder.remove();
    }
}
