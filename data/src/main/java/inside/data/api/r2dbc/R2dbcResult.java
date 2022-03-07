package inside.data.api.r2dbc;

import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class R2dbcResult implements Result {
    private final Result delegate;

    private R2dbcResult(Result delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static R2dbcResult of(Result result) {
        if (result instanceof R2dbcResult r) {
            return r;
        }
        return new R2dbcResult(result);
    }

    @Override
    public Mono<Integer> getRowsUpdated() {
        return Mono.from(delegate.getRowsUpdated());
    }

    @Override
    public <T> Flux<T> map(BiFunction<Row, RowMetadata, ? extends T> mappingFunction) {
        return Flux.from(delegate.map(mappingFunction));
    }

    public <T> Flux<T> mapWith(Function<? super R2dbcRow, ? extends T> mappingFunction) {
        return map((row, rowMetadata) -> mappingFunction.apply(R2dbcRow.of(row)));
    }

    @Override
    public <T> Flux<T> map(Function<? super Readable, ? extends T> mappingFunction) {
        return map((row, rowMetadata) -> mappingFunction.apply(row));
    }

    @Override
    public R2dbcResult filter(Predicate<Segment> filter) {
        delegate.filter(filter);
        return this;
    }

    @Override
    public <T> Flux<T> flatMap(Function<Segment, ? extends Publisher<? extends T>> mappingFunction) {
        return Flux.from(delegate.flatMap(mappingFunction));
    }
}
