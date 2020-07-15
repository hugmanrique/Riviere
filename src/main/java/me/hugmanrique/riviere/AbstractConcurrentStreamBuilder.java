package me.hugmanrique.riviere;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Concurrent {@code Stream} builder base implementation.
 *
 * @param <T> the type of the stream
 * @param <B> the type of the stream builders
 */
abstract class AbstractConcurrentStreamBuilder<T, B> {

    // This base implementation lazily allocates a set number of
    // buckets, i.e. thread-unsafe stream builders. Each caller is
    // assigned a particular bucket based on the current thread's
    // hashcode. The retrieval (and initialization) of the bucket
    // is performed atomically. However, to ensure memory visibility
    // the callers must acquire a lock on the bucket itself.
    // This lock is non-contested, making the acquisition overhead
    // negligible on modern JVMs.
    //
    // The buckets are not dereferenced after building the final stream.
    // This is based on the assumption that builder objects are short-lived
    // and are garbage collected after calling build().
    //
    // The level of expected concurrency can be customized via
    // the bucketCount parameter. It defaults to the number of
    // available processors.

    private static final int MAXIMUM_BUCKET_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int DEFAULT_BUCKET_COUNT = 16; // ConcurrentHashMap.DEFAULT_CAPACITY

    private final Supplier<? extends B> bucketSupplier;

    private final AtomicReferenceArray<B> buckets;
    private final int bucketCount;

    private final AtomicBoolean isBuilt = new AtomicBoolean(false);

    /**
     * Constructs a concurrent stream builder with {@link #DEFAULT_BUCKET_COUNT} buckets.
     * The supplier should be side-effect-free, since it may be called when
     * attempted updates fail due to contention among threads.
     *
     * @param bucketSupplier the side-effect-free stream builder supplier
     */
    protected AbstractConcurrentStreamBuilder(final Supplier<? extends B> bucketSupplier) {
        this(bucketSupplier, DEFAULT_BUCKET_COUNT);
    }

    /**
     * Constructs a concurrent stream builder with the given number of maximum buckets.
     * The supplier should be side-effect-free, since it may be called when
     * attempted updates fail due to contention among threads.
     *
     * @param bucketSupplier the side-effect-free stream builder supplier
     * @param bucketCount the maximum number of underlying stream builders
     * @throws IllegalArgumentException if {@code bucketCount} is non-positive
     */
    protected AbstractConcurrentStreamBuilder(final Supplier<? extends B> bucketSupplier,
                                              int bucketCount) {
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("Bucket count is non-positive, got " + bucketCount);
        }
        this.bucketSupplier = requireNonNull(bucketSupplier);
        this.bucketCount = Math.min(bucketCount, MAXIMUM_BUCKET_COUNT);
        this.buckets = new AtomicReferenceArray<>(this.bucketCount);
    }

    private void throwBuilt() {
        throw new IllegalStateException("Builder is in built state");
    }

    /**
     * Returns the builder bucket associated to the current thread.
     *
     * @return the current stream builder
     */
    protected final B get() {
        if (isBuilt.get()) {
            throwBuilt();
        }

        int index = Thread.currentThread().hashCode() % bucketCount;
        return buckets.updateAndGet(index, previous ->
                previous != null ? previous : bucketSupplier.get());
    }

    /**
     * Builds the stream, transitioning this builder to the built state.
     *
     * @return the built stream
     * @throws IllegalStateException if the builder has already transitioned to
     *         the built state
     */
    public final T build() {
        if (!isBuilt.compareAndSet(false, true)) {
            throwBuilt();
        }

        Stream<T> streams = IntStream.range(0, bucketCount)
                .mapToObj(buckets::get)
                .filter(Objects::nonNull)
                .map(builder -> {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (builder) {
                        return build0(builder);
                    }
                });

        return flatMap(streams);
    }

    /**
     * Builds a stream from the given builder.
     *
     * @param builder the builder
     * @return the built stream
     */
    protected abstract T build0(B builder);

    /**
     * Returns a stream consisting of the elements of all streams
     * contained in the given stream.
     *
     * @param streams the streams containing the elements
     * @return the new stream
     */
    protected abstract T flatMap(Stream<T> streams);
}
