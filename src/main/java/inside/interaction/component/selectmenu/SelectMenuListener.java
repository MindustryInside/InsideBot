package inside.interaction.component.selectmenu;

import inside.interaction.SelectMenuEnvironment;
import inside.interaction.component.InteractionListener;
import reactor.core.publisher.Mono;

public interface SelectMenuListener extends InteractionListener{

    Mono<Void> handle(SelectMenuEnvironment env);
}
