package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.Poll;
import inside.data.repository.*;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
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
}
