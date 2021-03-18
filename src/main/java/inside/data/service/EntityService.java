package inside.data.service;

import inside.data.entity.base.BaseEntity;

public interface EntityService<K, V extends BaseEntity>{

    V find(K id);

    void save(V entity);

    void delete(K id);

    void delete(V entity);
}
