package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.Poll;
import inside.data.repository.PollRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

@Service
public class PollService extends BaseLongObjEntityService<Poll, PollRepository>{

    protected PollService(PollRepository repository, Settings settings){
        super(repository, settings.getCache().isStarboard());
    }

    @Nullable
    @Override
    protected Poll find0(long id){
        return repository.findByMessageId(id);
    }

    @Transactional
    public Mono<Void> deleteAllByGuildId(long guildId){
        return Mono.fromRunnable(() -> repository.deleteAllByGuildId(guildId));
    }
}
