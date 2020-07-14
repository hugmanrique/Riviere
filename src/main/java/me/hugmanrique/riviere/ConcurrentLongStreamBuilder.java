package me.hugmanrique.riviere;

import java.util.stream.LongStream;

/**
 * A {@link LongStream.Builder} supporting full concurrency of additions.
 */
public final class ConcurrentLongStreamBuilder extends AbstractConcurrentStreamBuilder<LongStream, LongStream.Builder>
        implements LongStream.Builder {

    public ConcurrentLongStreamBuilder() {
        super(LongStream::builder);
    }

    @Override
    public void accept(final long value) {
        get().accept(value);
    }

    @Override
    protected LongStream build0() {
        return getAll()
                .map(LongStream.Builder::build)
                .flatMapToLong(s -> s);
    }
}
