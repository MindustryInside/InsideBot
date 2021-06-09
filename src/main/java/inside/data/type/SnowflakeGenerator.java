package inside.data.type;

import discord4j.common.util.Snowflake;
import inside.util.Strings;
import org.hibernate.*;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.*;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

public final class SnowflakeGenerator implements IdentifierGenerator, Configurable{
    public long timestamp;

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException{
        timestamp = Strings.parseLong(params.getProperty("timestamp"), Snowflake.DISCORD_EPOCH);
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException{
        return System.currentTimeMillis() - timestamp << 22;
    }
}
