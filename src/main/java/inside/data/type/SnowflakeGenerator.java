package inside.data.type;

import discord4j.common.util.Snowflake;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.time.Instant;

@SuppressWarnings("unused")
public final class SnowflakeGenerator implements IdentifierGenerator{
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException{
        return Snowflake.of(Instant.now()).asString();
    }
}
