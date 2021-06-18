package inside.data.repository;

import inside.data.entity.EmojiDispenser;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmojiDispenserRepository extends GuildRepository<EmojiDispenser>{

    EmojiDispenser findByMessageId(long messageId);
}
