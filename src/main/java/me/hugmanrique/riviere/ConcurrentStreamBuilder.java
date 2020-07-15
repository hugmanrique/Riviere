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

    public ConcurrentStreamBuilder() {
        super(Stream::builder);
    }

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
    protected Stream<T> build0(final Stream.Builder<T> builder) {
        return builder.build();
    }

    @Override
    protected Stream<T> flatMap(final Stream<Stream<T>> streams) {
        return streams.flatMap(s -> s);
    }
}
