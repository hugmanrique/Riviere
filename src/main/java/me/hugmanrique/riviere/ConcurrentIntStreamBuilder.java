package me.hugmanrique.riviere;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A {@link IntStream.Builder} supporting full concurrency of additions.
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public final class ConcurrentIntStreamBuilder extends AbstractConcurrentStreamBuilder<IntStream, IntStream.Builder>
        implements IntStream.Builder {

    public ConcurrentIntStreamBuilder() {
        super(IntStream::builder);
    }

    public ConcurrentIntStreamBuilder(final int bucketCount) {
        super(IntStream::builder, bucketCount);
    }

    @Override
    public void accept(final int value) {
        var builder = get();
        synchronized (builder) {
            builder.accept(value);
        }
    }

    @Override
    protected IntStream build0(final IntStream.Builder builder) {
        return builder.build();
    }

    @Override
    protected IntStream flatMap(final Stream<IntStream> streams) {
        return streams.flatMapToInt(s -> s);
    }
}
