package me.hugmanrique.riviere;

import java.util.stream.DoubleStream;

/**
 * A {@link DoubleStream.Builder} supporting full concurrency of additions.
 */
public final class ConcurrentDoubleStreamBuilder extends AbstractConcurrentStreamBuilder<DoubleStream, DoubleStream.Builder>
        implements DoubleStream.Builder {

    public ConcurrentDoubleStreamBuilder() {
        super(DoubleStream::builder);
    }

    @Override
    public void accept(final double value) {
        get().accept(value);
    }

    @Override
    protected DoubleStream build0() {
        return getAll()
                .map(DoubleStream.Builder::build)
                .flatMapToDouble(s -> s);
    }
}
