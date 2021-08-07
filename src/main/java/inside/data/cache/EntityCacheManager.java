package inside.data.cache;

import inside.data.entity.base.BaseEntity;

import java.util.function.Function;

public interface EntityCacheManager{

    <T extends BaseEntity, K> T get(Class<T> clazz, K id, Function<? super K, ? extends T> supplier);

    <T extends BaseEntity, K> void evict(Class<T> clazz, K id);
}
