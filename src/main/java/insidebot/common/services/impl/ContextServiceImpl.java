package insidebot.common.services.impl;

import discord4j.common.util.Snowflake;
 import insidebot.common.services.ContextService;
import insidebot.data.service.GuildService;
import insidebot.util.LocaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ContextServiceImpl implements ContextService{

    private final ThreadLocal<Locale> localeHolder = new NamedThreadLocal<>("ContextServiceImpl.Locale");

    private final ThreadLocal<Snowflake> guildHolder = new NamedThreadLocal<>("ContextServiceImpl.GuildIds");

    @Autowired
    private GuildService guildService;

    @Override
    public void init(Snowflake guildId){
        locale(localeOrDefault(guildId));
        guildHolder.set(guildId);
    }

    @Override
    public Locale locale(){
        Locale locale = localeHolder.get();
        if(locale == null){
            Snowflake guildId = guildHolder.get();
            if(guildId != null){
                localeOrDefault(guildService.locale(guildId));
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
    public Locale localeOrDefault(String locale){
        return LocaleUtil.getOrDefault(locale);
    }

    @Override
    public Locale localeOrDefault(Snowflake guildId){
        return LocaleUtil.getOrDefault(guildService.locale(guildId));
    }

    @Override
    public void reset(){
        guildHolder.remove();
        localeHolder.remove();
    }
}
