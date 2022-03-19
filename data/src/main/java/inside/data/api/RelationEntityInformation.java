package inside.data.api;

import inside.data.annotation.*;
import inside.util.Preconditions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RelationEntityInformation<T> {
    private final List<PersistentProperty> properties;

    private final String table;
    private final Class<T> type;

    private RelationEntityInformation(List<PersistentProperty> properties,
                                      String table, Class<T> type) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.table = Objects.requireNonNull(table, "table");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> RelationEntityInformation<T> compile(Class<T> type) {
        Preconditions.requireArgument(type.isAnnotationPresent(Entity.class) && Modifier.isAbstract(type.getModifiers()),
                () -> "Not a entity class " + type.getCanonicalName());

        List<PersistentProperty> properties = new ArrayList<>();

        Stream.concat(getMappedSuperclassMethods(type).stream(), Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> !method.isAnnotationPresent(Transient.class)
                        && method.getParameterCount() == 0
                        && (method.isAnnotationPresent(Column.class) ||
                        method.isAnnotationPresent(Id.class) ||
                        method.isAnnotationPresent(Generated.class))
                        && !Modifier.isStatic(method.getModifiers()))
                .distinct()
                .forEach(method -> properties.add(new PersistentPropertyImpl(method)));

        String table = Optional.ofNullable(type.getDeclaredAnnotation(Table.class))
                .map(Table::name)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(type::getSimpleName);

        return new RelationEntityInformation<>(properties, table, type);
    }

    private static void recursiveCollectMappedSuperclassMethods(Class<?> type, LinkedList<? super Class<?>> methods) {
        for (Class<?> anInterface : type.getInterfaces()) {
            if (anInterface.isAnnotationPresent(MapperSuperclass.class)) {
                methods.addFirst(anInterface);
                recursiveCollectMappedSuperclassMethods(anInterface, methods);
            }
        }
    }

    private static List<Method> getMappedSuperclassMethods(Class<?> type) {
        LinkedList<Method> superMethods = new LinkedList<>();
        LinkedList<Class<?>> superTypes = new LinkedList<>();
        recursiveCollectMappedSuperclassMethods(type, superTypes);
        for (Class<?> superType : superTypes) {
            for (Method method : superType.getDeclaredMethods()) {
                superMethods.addFirst(method);
            }
        }

        return superMethods;
    }

    public boolean isNew(T obj) {
        Object act = getIdProperty().getValue(obj);
        return act == null || act instanceof Number n && n.doubleValue() == -1;
    }

    public PersistentProperty getIdProperty() {
        return properties.stream()
                .filter(PersistentProperty::isId)
                .findFirst()
                .orElseThrow();
    }

    public List<PersistentProperty> getGeneratedProperties() {
        return properties.stream()
                .filter(PersistentProperty::isGenerated)
                .toList();
    }

    public List<PersistentProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public List<PersistentProperty> getCandidateProperties() {
        return properties.stream()
                .filter(prop -> !prop.isGenerated() && !prop.isId())
                .toList();
    }

    public String getTable() {
        return table;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RelationEntityInformation<?> that = (RelationEntityInformation<?>) o;
        return properties.equals(that.properties) && table.equals(that.table) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, table, type);
    }

    @Override
    public String toString() {
        return "RelationEntityInformation{" +
                "properties=" + properties +
                ", table='" + table + '\'' +
                ", type=" + type +
                '}';
    }
}
