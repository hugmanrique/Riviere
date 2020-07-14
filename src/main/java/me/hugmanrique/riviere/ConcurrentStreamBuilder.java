package me.hugmanrique.riviere;

import java.util.stream.Stream;

/**
 * A {@link Stream.Builder} supporting full concurrency of additions.
 *
 * @param <T> the type of the stream elements
 */
public final class ConcurrentStreamBuilder<T> extends AbstractConcurrentStreamBuilder<Stream<T>, Stream.Builder<T>>
        implements Stream.Builder<T> {

    public ConcurrentStreamBuilder() {
        super(Stream::builder);
    }

    @Override
    public void accept(final T value) {
        get().accept(value);
    }

    @Override
    protected Stream<T> build0() {
        return getAll().flatMap(Stream.Builder::build);
    }
}
