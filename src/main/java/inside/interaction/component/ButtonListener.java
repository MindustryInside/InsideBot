package inside.interaction.component;

import inside.interaction.ButtonInteractionEnvironment;
import org.reactivestreams.Publisher;

public interface ButtonListener extends ComponentListener {

    Publisher<?> handle(ButtonInteractionEnvironment env);
}
