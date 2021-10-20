package inside.interaction.component;

import inside.interaction.annotation.ComponentProvider;

import java.util.Objects;

public interface InteractionListener{

    default String getCustomId(){
        var ann = getClass().getAnnotation(ComponentProvider.class);
        Objects.requireNonNull(ann, "ann");
        return ann.value();
    }
}
