package inside.data.repository;

import inside.data.entity.CommandConfig;
import inside.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandConfigRepository extends GuildRepository<CommandConfig>{

    @Query(value = "select * from command_config where guild_id = :guildId and (aliases >| :name or names >| :name)", nativeQuery = true)
    CommandConfig findByAlias(long guildId, String name);
}
