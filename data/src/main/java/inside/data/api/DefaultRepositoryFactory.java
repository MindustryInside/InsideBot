package inside.data.api;

import inside.data.DatabaseResources;
import inside.data.repository.base.ReactiveRepository;
import inside.data.repository.support.BaseRepository;
import inside.data.repository.support.Repository;
import inside.data.repository.support.RepositoryInvocationHandler;
import inside.util.Preconditions;

import java.lang.reflect.Proxy;
import java.util.Objects;

public class DefaultRepositoryFactory implements RepositoryFactory {

    private final DatabaseResources databaseResources;

    public DefaultRepositoryFactory(DatabaseResources databaseResources) {
        this.databaseResources = Objects.requireNonNull(databaseResources, "databaseResources");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, T, R extends ReactiveRepository<K, T>> R create(Class<R> type) {
        Preconditions.requireState(type.isAnnotationPresent(Repository.class), "Not a repository.");

        Class<?> entityType = ClassTypeInformation.from(type)
                .getRequiredSuperTypeInformation(ReactiveRepository.class)
                .getTypeArguments()
                .get(1).getType();

        return (R) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{type},
                new RepositoryInvocationHandler(new BaseRepository<>(databaseResources, entityType)));
    }
}
