package inside.data.api.r2dbc;

import io.r2dbc.spi.Batch;
import reactor.core.publisher.Flux;

public class R2dbcBatch implements Batch {
    private final Batch delegate;

    public R2dbcBatch(Batch delegate) {
        this.delegate = delegate;
    }

    @Override
    public R2dbcBatch add(String sql) {
        delegate.add(sql);
        return this;
    }

    @Override
    public Flux<R2dbcResult> execute() {
        return Flux.from(delegate.execute()).map(R2dbcResult::of);
    }
}
