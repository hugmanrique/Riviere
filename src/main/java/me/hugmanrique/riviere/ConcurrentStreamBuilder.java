package me.hugmanrique.riviere;

import java.util.stream.Stream;

/**
 * A {@link Stream.Builder} supporting full concurrency of additions.
 *
 * @param <T> the type of the stream elements
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public final class ConcurrentStreamBuilder<T> extends AbstractConcurrentStreamBuilder<Stream<T>, Stream.Builder<T>>
        implements Stream.Builder<T> {

    /**
     * Constructs a concurrent {@link Stream} builder.
     */
    public ConcurrentStreamBuilder() {
        super(Stream::builder);
    }

    /**
     * Constructs a concurrent {@link Stream} builder with the given number
     * of maximum buckets.
     *
     * @param bucketCount the maximum number of underlying stream builders
     */
    public ConcurrentStreamBuilder(final int bucketCount) {
        super(Stream::builder, bucketCount);
    }

    @Override
    public void accept(final T value) {
        var builder = get();
        synchronized (builder) {
            builder.accept(value);
        }
    }

    @Override
    Stream<T> build0(final Stream.Builder<T> builder) {
        return builder.build();
    }

    @Override
    Stream<T> flatMap(final Stream<Stream<T>> streams) {
        return streams.flatMap(s -> s);
    }
}
