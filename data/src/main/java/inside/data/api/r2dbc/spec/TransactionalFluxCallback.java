package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.R2dbcConnection;
import org.reactivestreams.Publisher;

@FunctionalInterface
public interface TransactionalFluxCallback<T> extends TransactionalCallback<T> {
    @Override
    Publisher<T> execute(R2dbcConnection connection);
}
