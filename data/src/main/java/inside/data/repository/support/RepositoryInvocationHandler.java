package inside.data.repository.support;

import inside.data.api.EntityRowMapper;
import inside.data.api.PropertyRowMapper;
import inside.data.api.QueryUtil;
import inside.data.api.r2dbc.RowMapper;
import inside.data.api.r2dbc.spec.ExecuteSpec;
import inside.util.Reflect;
import io.r2dbc.spi.IsolationLevel;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

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
        if (log.isTraceEnabled()) {
            log.trace("Method: {}", method);
            log.trace("Args: {}", Arrays.deepToString(args));
        }

        for (var inter : baseRepository.getClass().getInterfaces()) {
            if (inter == method.getDeclaringClass()) {
                return Reflect.invoke(method, baseRepository, args);
            }
        }

        String sql = QueryUtil.parseSql(method, baseRepository.info);
        if (log.isTraceEnabled()) {
            log.trace("Sql: {}", sql);
        }

        boolean isCount = QueryUtil.isCountQuery(sql);

        boolean isMultiple = Flux.class.equals(method.getReturnType()) &&
                method.getName().toLowerCase(Locale.ROOT).contains("all");

        boolean ignoreElements;
        Type returnTypeUnwrapped = null;
        if (method.getGenericReturnType() instanceof ParameterizedType p) {
            var typeParameters = p.getActualTypeArguments();
            ignoreElements = typeParameters.length > 0 &&
                    (returnTypeUnwrapped = typeParameters[0]) instanceof Class<?> c &&
                    c == Void.class;
        } else {
            throw new IllegalStateException("Not a reactive return type..");
        }

        RowMapper<?> mapper = !isSubtypeOrType(returnTypeUnwrapped, baseRepository.type)
                ? PropertyRowMapper.create(Reflect.toClass(returnTypeUnwrapped))
                : EntityRowMapper.create(baseRepository.info, baseRepository.databaseResources.getEntityOperations());

        var executeSpec = baseRepository.databaseResources
                .getDatabaseClient().sql(sql)
                .transactional(IsolationLevel.READ_COMMITTED);

        executeSpec = bindParameters(executeSpec, args);

        if (ignoreElements) {
            return executeSpec.then();
        }

        if (isCount) {
            return executeSpec.map(row -> row.getLong(0)).one();
        }

        var fetchSpec = executeSpec.map(mapper);
        if (isMultiple) {
            return fetchSpec.all();
        }
        return fetchSpec.one();
    }

    private static boolean isSubtypeOrType(@Nullable Type type0, @Nullable Type type1) {
        if (type0 == null || type1 == null) {
            return false;
        }
        if (type0.equals(type1)) {
            return true;
        }
        if (type0 instanceof Class<?> c0 && type1 instanceof Class<?> c1) {
            return c0.isAssignableFrom(c1);
        }
        if (type0 instanceof GenericArrayType g) {
            return isSubtypeOrType(g.getGenericComponentType(), type1);
        }
        if (type0 instanceof ParameterizedType p && type1 instanceof ParameterizedType p1) {
            Type[] pArgs = p.getActualTypeArguments();
            Type[] p1Args = p1.getActualTypeArguments();
            for (Type pArg : pArgs) {
                for (Type p1Arg : p1Args) {
                    if (isSubtypeOrType(pArg, p1Arg)) {
                        return true;
                    }
                }
            }
        }
        if (type0 instanceof WildcardType w) {
            return Arrays.stream(w.getLowerBounds()).anyMatch(t -> isSubtypeOrType(t, type1)) ||
                    Arrays.stream(w.getUpperBounds()).anyMatch(t -> isSubtypeOrType(t, type1));
        }
        if (type0 instanceof TypeVariable<?> tv) {
            for (Type t : tv.getBounds()) {
                if (isSubtypeOrType(t, type1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
