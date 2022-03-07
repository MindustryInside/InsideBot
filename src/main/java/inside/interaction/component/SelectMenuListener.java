package inside.interaction.component;

import inside.interaction.SelectMenuInteractionEnvironment;
import org.reactivestreams.Publisher;

public interface SelectMenuListener extends ComponentListener {

    Publisher<?> handle(SelectMenuInteractionEnvironment event);
}
