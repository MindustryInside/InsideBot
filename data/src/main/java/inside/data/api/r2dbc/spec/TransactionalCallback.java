package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.R2dbcConnection;
import org.reactivestreams.Publisher;

public interface TransactionalCallback<T> {

    Publisher<T> execute(R2dbcConnection connection);
}
