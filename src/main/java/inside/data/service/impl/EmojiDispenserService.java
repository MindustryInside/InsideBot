package inside.data.service.impl;

import discord4j.store.api.util.LongLongTuple2;
import inside.Settings;
import inside.data.entity.EmojiDispenser;
import inside.data.repository.EmojiDispenserRepository;
import inside.data.service.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.util.annotation.Nullable;

@Service
public class EmojiDispenserService extends BaseEntityService<LongLongTuple2, EmojiDispenser, EmojiDispenserRepository>{

    protected EmojiDispenserService(EmojiDispenserRepository repository, Settings settings){
        super(repository, settings.getCache().isEmojiDispenser());
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    protected EmojiDispenser find0(LongLongTuple2 id){
        long messageId = id.getT1();
        long roleId = id.getT2();
        return repository.findByMessageIdAndRoleId(messageId, roleId);
    }

    @Override
    protected Object extractId(EmojiDispenser entity){
        return LongLongTuple2.of(entity.getMessageId().asLong(), entity.getRoleId().asLong());
    }

    @Transactional(readOnly = true)
    public Mono<Long> countAllByGuildId(long guildId){
        return Mono.fromSupplier(() -> repository.countAllByGuildId(guildId));
    }

    @Transactional(readOnly = true)
    public Flux<EmojiDispenser> getAllByMessageId(long messageId){
        return Flux.defer(() -> Flux.fromIterable(repository.findAllByMessageId(messageId)));
    }

    @Transactional(readOnly = true)
    public Flux<EmojiDispenser> getAllByGuildId(long guildId){
        return Flux.defer(() -> Flux.fromIterable(repository.findAllByGuildId(guildId)));
    }

    @Transactional
    public Mono<Void> deleteAllByGuildId(long guildId){
        return Mono.fromRunnable(() -> repository.deleteAllByGuildId(guildId));
    }
}
