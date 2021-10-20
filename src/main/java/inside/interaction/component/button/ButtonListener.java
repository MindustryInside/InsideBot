package inside.interaction.component.button;

import inside.interaction.ButtonEnvironment;
import inside.interaction.component.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface ButtonListener extends InteractionListener{

    Publisher<?> handle(ButtonEnvironment env);
}
