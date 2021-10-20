package inside.interaction.component.selectmenu;

import inside.interaction.SelectMenuEnvironment;
import inside.interaction.component.InteractionListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface SelectMenuListener extends InteractionListener{

    Publisher<?> handle(SelectMenuEnvironment env);
}
