package me.hugmanrique.riviere;

import java.util.stream.IntStream;

/**
 * A {@link IntStream.Builder} supporting full concurrency of additions.
 */
public final class ConcurrentIntStreamBuilder extends AbstractConcurrentStreamBuilder<IntStream, IntStream.Builder>
        implements IntStream.Builder {

    public ConcurrentIntStreamBuilder() {
        super(IntStream::builder);
    }

    @Override
    public void accept(final int value) {
        get().accept(value);
    }

    @Override
    protected IntStream build0() {
        return getAll()
                .map(IntStream.Builder::build)
                .flatMapToInt(s -> s);
    }
}
