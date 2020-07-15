package me.hugmanrique.riviere;

import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * A {@link DoubleStream.Builder} supporting full concurrency of additions.
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public final class ConcurrentDoubleStreamBuilder extends AbstractConcurrentStreamBuilder<DoubleStream, DoubleStream.Builder>
        implements DoubleStream.Builder {

    /**
     * Constructs a concurrent {@link DoubleStream} builder.
     */
    public ConcurrentDoubleStreamBuilder() {
        super(DoubleStream::builder);
    }

    /**
     * Constructs a concurrent {@link DoubleStream} builder with the given number
     * of maximum buckets.
     *
     * @param bucketCount the maximum number of underlying stream builders
     */
    public ConcurrentDoubleStreamBuilder(final int bucketCount) {
        super(DoubleStream::builder, bucketCount);
    }

    @Override
    public void accept(final double value) {
        var builder = get();
        synchronized (builder) {
            builder.accept(value);
        }
    }

    @Override
    protected DoubleStream build0(final DoubleStream.Builder builder) {
        return builder.build();
    }

    @Override
    protected DoubleStream flatMap(final Stream<DoubleStream> streams) {
        return streams.flatMapToDouble(s -> s);
    }
}
