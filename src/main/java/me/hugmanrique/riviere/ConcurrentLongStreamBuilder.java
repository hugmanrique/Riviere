package me.hugmanrique.riviere;

import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A {@link LongStream.Builder} supporting full concurrency of additions.
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public final class ConcurrentLongStreamBuilder extends AbstractConcurrentStreamBuilder<LongStream, LongStream.Builder>
        implements LongStream.Builder {

    public ConcurrentLongStreamBuilder() {
        super(LongStream::builder);
    }

    public ConcurrentLongStreamBuilder(final int bucketCount) {
        super(LongStream::builder, bucketCount);
    }

    @Override
    public void accept(final long value) {
        var builder = get();
        synchronized (builder) {
            builder.accept(value);
        }
    }

    @Override
    protected LongStream build0(final LongStream.Builder builder) {
        return builder.build();
    }

    @Override
    protected LongStream flatMap(final Stream<LongStream> streams) {
        return streams.flatMapToLong(s -> s);
    }
}
