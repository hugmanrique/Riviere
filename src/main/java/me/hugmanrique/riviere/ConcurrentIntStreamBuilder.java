package me.hugmanrique.riviere;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A {@link IntStream.Builder} supporting full concurrency of additions.
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public final class ConcurrentIntStreamBuilder extends AbstractConcurrentStreamBuilder<IntStream, IntStream.Builder>
        implements IntStream.Builder {

    /**
     * Constructs a concurrent {@link IntStream} builder.
     */
    public ConcurrentIntStreamBuilder() {
        super(IntStream::builder);
    }

    /**
     * Constructs a concurrent {@link IntStream} builder with the given number
     * of maximum buckets.
     *
     * @param bucketCount the maximum number of underlying stream builders
     */
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
    IntStream build0(final IntStream.Builder builder) {
        return builder.build();
    }

    @Override
    IntStream flatMap(final Stream<IntStream> streams) {
        return streams.flatMapToInt(s -> s);
    }
}
