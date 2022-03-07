package inside.data.api.r2dbc.spec;

import reactor.core.publisher.Mono;

public interface UpdatedRowsFetchSpec {

    Mono<Integer> rowsUpdated();
}
