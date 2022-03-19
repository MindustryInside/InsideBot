package inside.data.api;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Query;
import inside.util.Mathf;
import inside.util.Preconditions;
import inside.util.Reflect;
import org.reactivestreams.Publisher;
import reactor.util.function.Tuple2;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class QueryUtil {

    public static final String SELECT_ALL_QUERY_STRING = "select * from %s";
    public static final String SELECT_COUNT_QUERY_STRING = "select count(*) from %s";

    public static final Pattern andPattern = Pattern.compile("And");
    public static final Pattern isCountQueryPattern = Pattern.compile("^(.+count.+)$", Pattern.CASE_INSENSITIVE);

    private static final String selfAlias = "self";
    private static final Map<List<String>, String> sqlAliases = Map.of(
            List.of("find", "findAll", "get", "getAll"), "select",
            List.of("count"), "select count(*)",
            List.of("delete", "deleteAll"), "delete");

    private static final Pattern prefixPattern = Pattern.compile("^(" + sqlAliases.keySet().stream()
                    .map(list -> String.join("|", list))
                    .collect(Collectors.joining("|")) + ")(.+)$",
            Pattern.CASE_INSENSITIVE);

    private QueryUtil() {

    }

    public static boolean isCountQuery(CharSequence str) {
        return isCountQueryPattern.matcher(str).find();
    }

    public static <T> String createSelectAllSql(RelationEntityInformation<T> info) {
        return String.format(SELECT_ALL_QUERY_STRING, info.getTable());
    }

    public static <T extends Member & AnnotatedElement> String getName(T element) {
        return Optional.ofNullable(element.getDeclaredAnnotation(Column.class))
                .map(Column::name)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(element::getName);
    }

    public static <T> String createDeleteSql(RelationEntityInformation<T> info) {
        StringBuilder builder = new StringBuilder();
        builder.append("delete from ");
        builder.append(info.getTable());

        builder.append(" where ");
        var idProperty = info.getIdProperty();
        builder.append(idProperty.getName()).append(" = $1");

        return builder.toString();
    }

    public static <T> ParseResult parseSql(Method method, RelationEntityInformation<T> info) {
        Objects.requireNonNull(method, "method");
        Preconditions.requireArgument(!Modifier.isStatic(method.getModifiers()));
        Preconditions.requireArgument(Modifier.isAbstract(method.getDeclaringClass().getModifiers()));
        if (!(method.getGenericReturnType() instanceof ParameterizedType r &&
                Publisher.class.isAssignableFrom(Reflect.toClass(r.getRawType())))) {
            throw new IllegalStateException();
        }

        Type ret = r.getActualTypeArguments()[0];
        Query queryMeta = method.getDeclaredAnnotation(Query.class); // Пользовательский sql
        List<Class<?>> returns = List.of(info.getType());
        if (queryMeta != null) {
            if (ret instanceof ParameterizedType tp && Tuple2.class.isAssignableFrom(Reflect.toClass(tp.getRawType()))) {
                returns = Stream.of(tp.getActualTypeArguments())
                        .map(Reflect::toClass)
                        .collect(Collectors.toUnmodifiableList());

                Preconditions.requireState(returns.size() <= 8, "Incorrect props count to return, max is 8");
            } else if (ret instanceof Class<?> c) {
                returns = List.of(c);
            }

            return new ParseResult(queryMeta.value(), returns);
        }

        Preconditions.requireArgument(prefixPattern.matcher(method.getName()).find(),
                () -> "No sql-named method. Method: " + method);

        StringBuilder builder = new StringBuilder();
        String prefix = sqlAliases.entrySet().stream()
                .filter(entry -> entry.getKey().stream()
                        .anyMatch(method.getName()::startsWith))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow();

        String query = sqlAliases.keySet().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(method.getName()::startsWith)
                .findFirst()
                .map(s -> method.getName().substring(s.length()))
                .orElseThrow();

        builder.append(prefix);

        int byBegin = query.indexOf("By");

        String propsSubstr = byBegin != -1 ? query.substring(0, byBegin) : ""; // то что возвращаем
        String querySubstr = byBegin != -1 ? query.substring(byBegin + 2) : query; // параметры

        if (!propsSubstr.isEmpty()) { // выборка конкретных полей
            if (!(ret instanceof ParameterizedType tp &&
                    Tuple2.class.isAssignableFrom(Reflect.toClass(tp.getRawType())))) {
                throw new IllegalStateException();
            }

            var props = info.getProperties().stream()
                    .collect(Collectors.toMap(p -> p.getName().toLowerCase(Locale.ROOT), Function.identity()));

            StringJoiner retpr = new StringJoiner(", ");
            returns = andPattern.splitAsStream(propsSubstr)
                    .map(rn -> rn.toLowerCase(Locale.ROOT))
                    .map(s -> {
                        if (s.equals(selfAlias)) {
                            info.getProperties().stream()
                                    .map(PersistentProperty::getName)
                                    .forEach(retpr::add);

                            return info.getType();
                        }

                        PersistentProperty p = props.get(s);
                        Preconditions.requireState(p != null, () -> "No persistent properties with name '" +
                                s + "' in the class '" + info.getType().getCanonicalName() + "' found");

                        retpr.add(p.getName());
                        return p.getClassType();
                    })
                    .collect(Collectors.toUnmodifiableList());

            Preconditions.requireState(returns.size() <= 8, "Incorrect props count to return, max is 8");

            builder.append(" ").append(retpr);
        } else if (prefix.equals("select")) {
            builder.append(" *");
        }

        builder.append(" from ");
        builder.append(info.getTable());

        if (method.getParameterCount() > 0 && !querySubstr.isEmpty()) {

            builder.append(" where ");
            if (querySubstr.contains("And")) {
                String[] parts = andPattern.split(querySubstr);
                Preconditions.requireArgument(parts.length == method.getParameterCount(), () ->
                        "Malformed parameters count for method " + method);

                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    PersistentProperty targetProp = info.getProperties().stream()
                            .filter(prop -> eq(prop, part))
                            .findFirst()
                            .orElseThrow();

                    builder.append(targetProp.getName()).append(" = $").append(i + 1);
                    if (i + 1 != parts.length) {
                        builder.append(" and ");
                    }
                }
            } else {
                PersistentProperty targetProp = info.getProperties().stream()
                        .filter(prop -> eq(prop, querySubstr))
                        .findFirst()
                        .orElseThrow();

                builder.append(targetProp.getName()).append(" = $1");
            }
        }

        String sql = builder.toString();

        return new ParseResult(sql, returns);
    }

    private static boolean eq(PersistentProperty prop, String query) {
        String fixed = Character.toLowerCase(query.charAt(0)) + query.substring(1);
        return prop.getMethod().getName().equals(fixed);
    }

    public static <T> String createUpdateSql(RelationEntityInformation<T> info) {
        StringBuilder builder = new StringBuilder();
        builder.append("update ");
        builder.append(info.getTable());

        builder.append(" set ");

        var candidateProperties = info.getCandidateProperties();
        for (int i = 0; i < candidateProperties.size(); i++) {
            var property = candidateProperties.get(i);

            builder.append(property.getName());
            builder.append(" = $").append(i + 1);
            if (i != candidateProperties.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(" where ");
        var idProperty = info.getIdProperty();
        builder.append(idProperty.getName()).append(" = $").append(candidateProperties.size() + 1);

        return builder.toString();
    }

    public static <T> String createInsertSql(RelationEntityInformation<T> info) {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into ");
        builder.append(info.getTable());
        builder.append(" (");

        var candidateProperties = info.getCandidateProperties();
        StringBuilder params = new StringBuilder(candidateProperties.size() * 2);
        for (int i = 0; i < candidateProperties.size(); i++) {
            var property = candidateProperties.get(i);

            builder.append(property.getName());
            params.append("$").append(i + 1);
            if (i != candidateProperties.size() - 1) {
                builder.append(", ");
                params.append(", ");
            }
        }

        builder.append(") values (");
        builder.append(params);
        builder.append(")");

        return builder.toString();
    }

    private static String generateAlias(RelationEntityInformation<?> info) {
        return info.getTable().charAt(0) + Integer.toHexString(Mathf.random.nextInt());
    }

    public static <T> String createSelectSql(RelationEntityInformation<T> info, EntityOperations entityOperations) {
        StringBuilder builder = new StringBuilder();
        builder.append("select * from ");
        builder.append(info.getTable());

        String alias = "";
        var candidateProperties = info.getCandidateProperties();
        for (PersistentProperty property : candidateProperties) {
            if (!property.getClassType().isAnnotationPresent(Entity.class)) {
                continue;
            }

            if (alias.isEmpty()) {
                alias = generateAlias(info);
                builder.append(" as ").append(alias);
            }

            var finfo = entityOperations.getInformation(property.getClassType());
            String falias = generateAlias(finfo);

            builder.append(" left join ").append(finfo.getTable());
            builder.append(" as ").append(falias);
            builder.append(" on ");

            builder.append(alias).append('.').append(property.getName()).append(" = ");
            builder.append(falias).append('.').append(property.getColumn()
                    .map(Column::referencedColumnName)
                    .filter(Predicate.not(String::isBlank))
                    .orElseThrow());
        }

        builder.append(" where ");
        var idProperty = info.getIdProperty();
        builder.append(idProperty.getName()).append(" = $1");

        return builder.toString();
    }

    public static <T> String createCountSql(RelationEntityInformation<T> info) {
        return String.format(SELECT_COUNT_QUERY_STRING, info.getTable());
    }

    public record ParseResult(String sql, List<Class<?>> returns) {}
}
