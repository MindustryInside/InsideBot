package inside.data.cache;

import com.github.benmanes.caffeine.cache.*;
import inside.data.entity.base.BaseEntity;

import java.util.Objects;
import java.util.function.Function;

public class CaffeineEntityCacheManager implements EntityCacheManager{

    private final Function<Caffeine<?, ?>, Caffeine<?, ?>> provider;
    // name->cache<id->obj>
    private final Cache<String, Cache<?, ?>> caches;

    public CaffeineEntityCacheManager(Function<Caffeine<?, ?>, Caffeine<?, ?>> provider){
        this.provider = Objects.requireNonNull(provider, "provider");
        caches = createCache();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> createCache(){
        return (Cache<K, V>)provider.apply(Caffeine.newBuilder()).build();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> getCache(String name){
        return (Cache<K, V>)caches.get(name, s -> createCache());
    }

    @Override
    public <T extends BaseEntity, K> T get(Class<T> clazz, K id, Function<? super K, ? extends T> supplier){
        Cache<K, T> cache = getCache(clazz.getName());
        return cache.get(id, supplier);
    }

    @Override
    public <T extends BaseEntity, K> void evict(Class<T> clazz, K id){
        Cache<K, T> cache = getCache(clazz.getName());
        cache.invalidate(id);
    }
}
