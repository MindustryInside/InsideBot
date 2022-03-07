package inside.data.api;

import inside.data.repository.base.ReactiveRepository;

public interface RepositoryFactory {

    <K, T, R extends ReactiveRepository<K, T>> R create(Class<R> type);
}
