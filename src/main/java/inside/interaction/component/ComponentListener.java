package inside.interaction.component;

import inside.interaction.annotation.ComponentProvider;

import java.util.Objects;

public interface ComponentListener {

    default String getCustomId() {
        ComponentProvider ann = getClass().getAnnotation(ComponentProvider.class);
        Objects.requireNonNull(ann, "ann");
        return ann.value();
    }
}
