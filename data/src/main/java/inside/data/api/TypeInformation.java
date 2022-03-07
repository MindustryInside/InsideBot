package inside.data.api;

import reactor.util.annotation.Nullable;

import java.util.List;

public interface TypeInformation<S> {

    Class<S> getType();

    @Nullable
    TypeInformation<?> getSuperTypeInformation(Class<?> superType);

    default TypeInformation<?> getRequiredSuperTypeInformation(Class<?> superType) {
        TypeInformation<?> result = getSuperTypeInformation(superType);

        if (result == null) {
            throw new IllegalArgumentException("Can't retrieve super type information for " + superType);
        }
        return result;
    }

    List<? extends TypeInformation<?>> getTypeArguments();
}
