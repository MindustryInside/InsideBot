package inside.data.api;

import inside.data.annotation.Column;
import inside.data.annotation.Query;
import inside.util.Preconditions;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class QueryUtil {

    public static final String SELECT_ALL_QUERY_STRING = "select * from %s";
    public static final String SELECT_COUNT_QUERY_STRING = "select count(*) from %s";

    public static final Pattern isCountQueryPattern = Pattern.compile("^(.+count.+)$", Pattern.CASE_INSENSITIVE);

    private static final Map<List<String>, String> sqlAliases = Map.of(
            List.of("findAll", "findAllBy", "findBy", "getAllBy", "getBy"), "select *",
            List.of("count", "countBy"), "select count(*)",
            List.of("delete", "deleteBy", "deleteAllBy"), "delete");

    private static final Pattern prefixPattern = Pattern.compile("^(" + sqlAliases.keySet().stream()
                    .map(list -> String.join("|", list))
                    .collect(Collectors.joining("|")) + ").*",
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
        var idProperties = info.getIdProperties();
        for (int i = 0; i < idProperties.size(); i++) {
            var property = idProperties.get(i);

            builder.append(property.getName());
            builder.append(" = $").append(i + 1);
            if (i != idProperties.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public static <T> String parseSql(Method method, RelationEntityInformation<T> info) {
        Objects.requireNonNull(method, "method");
        Preconditions.requireArgument(!Modifier.isStatic(method.getModifiers()));
        Preconditions.requireArgument(Modifier.isAbstract(method.getDeclaringClass().getModifiers()));

        Query queryMeta = method.getDeclaredAnnotation(Query.class); // Пользовательский sql
        if (queryMeta != null) {
            return queryMeta.value();
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
        builder.append(" from ");

        builder.append(info.getTable());
        if (method.getParameterCount() > 0) {

            builder.append(" where ");
            if (query.contains("And")) {
                String[] parts = query.split("And");
                Preconditions.requireArgument(parts.length == method.getParameterCount(), () ->
                        "Malformed parameters count for method " + method);

                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    PersistentProperty targetProperty = info.getProperties().stream()
                            .filter(prop -> eq(prop, part))
                            .findFirst()
                            .orElseThrow();

                    builder.append(targetProperty.getName()).append(" = $").append(i + 1);
                    if (i + 1 != parts.length) {
                        builder.append(" and ");
                    }
                }
            } else {
                PersistentProperty targetProperty = info.getProperties().stream()
                        .filter(prop -> eq(prop, query))
                        .findFirst()
                        .orElseThrow();

                builder.append(targetProperty.getName()).append(" = $1");
            }
        }

        return builder.toString();
    }

    private static boolean eq(PersistentProperty prop, String query) {
        String fixed = Character.toLowerCase(query.charAt(0)) + query.substring(1);
        if (prop instanceof FieldPersistentProperty f) {
            return f.getField().getName().equals(fixed);
        } else if (prop instanceof MethodPersistentProperty m) {
            return m.getMethod().getName().equals(fixed);
        } else {
            throw new IllegalStateException("Not a valid persistent property type: " + prop.getClass());
        }
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
        var idProperties = info.getIdProperties();
        for (int i = 0, offset = candidateProperties.size(); i < idProperties.size(); i++) {
            var property = idProperties.get(i);

            builder.append(property.getName());
            builder.append(" = $").append(i + offset + 1);
            if (i != idProperties.size() - 1) {
                builder.append(", ");
            }
        }

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

    public static <T> String createSelectSql(RelationEntityInformation<T> info) {
        StringBuilder builder = new StringBuilder();
        builder.append("select * from ");
        builder.append(info.getTable());

        builder.append(" where ");
        var idProperties = info.getIdProperties();
        for (int i = 0; i < idProperties.size(); i++) {
            var property = idProperties.get(i);

            builder.append(property.getName());
            builder.append(" = $").append(i + 1);
            if (i != idProperties.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    public static <T> String createCountSql(RelationEntityInformation<T> info) {
        return String.format(SELECT_COUNT_QUERY_STRING, info.getTable());
    }
}
