package inside.data.repository.support;

import inside.data.api.CompositeRowMapper;
import inside.data.api.EntityRowMapper;
import inside.data.api.PropertiesRowMapper;
import inside.data.api.QueryUtil;
import inside.data.api.QueryUtil.ParseResult;
import inside.data.api.r2dbc.RowMapper;
import inside.data.api.r2dbc.spec.ExecuteSpec;
import inside.util.Preconditions;
import inside.util.Reflect;
import io.r2dbc.spi.IsolationLevel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepositoryInvocationHandler implements InvocationHandler {

    private static final Logger log = Loggers.getLogger(RepositoryInvocationHandler.class);

    private final BaseRepository<?, ?> baseRepository;

    public RepositoryInvocationHandler(BaseRepository<?, ?> baseRepository) {
        this.baseRepository = Objects.requireNonNull(baseRepository, "baseRepository");
    }

    private static ExecuteSpec bindParameters(ExecuteSpec executeSpec, @Nullable Object[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                executeSpec = executeSpec.bindOptional(i, arg, Reflect.wrapIfPrimitive(arg.getClass()));
            }
        }

        return executeSpec;
    }

    @Override
    public Object invoke(Object proxy, Method method, @Nullable Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return args != null && proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
                case "toString": return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
            }
        }

        for (var inter : baseRepository.getClass().getInterfaces()) {
            if (inter == method.getDeclaringClass()) {
                return Reflect.invoke(method, baseRepository, args);
            }
        }

        Preconditions.requireState(Publisher.class.isAssignableFrom(method.getReturnType()));

        ParseResult res = QueryUtil.parseSql(method, baseRepository.info);
        if (log.isTraceEnabled()) {
            log.trace("Parse result: {} for {} (args: {})", res, method,
                    args != null ? Arrays.deepToString(args) : null);
        }

        boolean ignoreElements;
        if (method.getGenericReturnType() instanceof ParameterizedType p) {
            var typeParameters = p.getActualTypeArguments();
            ignoreElements = typeParameters.length > 0 && typeParameters[0] instanceof Class<?> c && c == Void.class;
        } else {
            // Подтверждается выше
            throw new IllegalStateException();
        }

        var executeSpec = baseRepository.databaseResources
                .getDatabaseClient().sql(res.sql())
                .transactional(IsolationLevel.READ_COMMITTED);

        executeSpec = bindParameters(executeSpec, args);

        if (ignoreElements) {
            return executeSpec.then();
        }

        if (QueryUtil.isCountQuery(res.sql())) {
            return executeSpec.map(row -> row.getLong(0)).one();
        }

        RowMapper<?> mapper;
        if (res.returns().size() == 1 && baseRepository.type.isAssignableFrom(res.returns().get(0))) {
            mapper = EntityRowMapper.create(baseRepository.info, baseRepository.databaseResources.getEntityOperations());
        } else {
            boolean hasObj = res.returns().stream().anyMatch(baseRepository.type::isAssignableFrom);
            if (hasObj) {
                mapper = CompositeRowMapper.create(res.returns().stream()
                        .map(c -> {
                            if (baseRepository.type.isAssignableFrom(c)) {
                                return EntityRowMapper.create(baseRepository.info,
                                        baseRepository.databaseResources.getEntityOperations());
                            }

                            return PropertiesRowMapper.create(List.of(c));
                        })
                        .collect(Collectors.toList()));
            } else {
                mapper = PropertiesRowMapper.create(res.returns());
            }
        }

        var fetchSpec = executeSpec.map(mapper);
        if (Flux.class.isAssignableFrom(method.getReturnType())) {
            return fetchSpec.all();
        }
        return fetchSpec.one();
    }
}
