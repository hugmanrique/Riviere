package me.hugmanrique.riviere;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Concurrent {@link Stream} builder base implementation.
 *
 * @param <T> the type of the stream
 * @param <B> the type of the underlying stream builders
 */
abstract class AbstractConcurrentStreamBuilder<T, B> {

    // Each thread has its own underlying stream builder.
    // This allows the addition of elements without locking.
    // When build() is invoked, all builders create their
    // own stream instances, which are then concatenated
    // using flatMap, effectively synchronizing the state of
    // all stream builders.
    //
    // The builders collection contains every initialized
    // segment builder. Care must be given to avoid memory leaks
    // caused by dangling thread-local builder references.
    // The Stream.Builder contract specifies the builder can only
    // be used once (all successive invocations throw IllegalStateException).
    // As so, we clear every builder reference after build().
    // This has the downside of preventing threads from being
    // garbage collected until the build() method is invoked.
    // However, this is hardly ever a concern, since builder
    // objects are short-lived.
    // Moreover, the alternative — wrapping tracked builders with
    // WeakReference would drop all elements added by a thread
    // that is later stopped.

    private final ThreadLocal<B> builder;
    private final Collection<B> builders;

    private final AtomicBoolean isBuilt = new AtomicBoolean(false);

    protected AbstractConcurrentStreamBuilder(final Supplier<? extends B> supplier) {
        requireNonNull(supplier);
        this.builders = new CopyOnWriteArrayList<>();
        this.builder = ThreadLocal.withInitial(() -> {
            var builder = requireNonNull(supplier.get(), "supplied builder");
            builders.add(builder);

            return builder;
        });
    }

    private void throwBuilt() {
        throw new IllegalStateException("Builder is in built state");
    }

    protected B get() {
        if (isBuilt.get()) {
            throwBuilt();
        }

        return builder.get();
    }

    protected Stream<B> getAll() {
        return builders.stream();
    }

    /**
     * Builds the stream, transitioning this builder to the built state.
     *
     * @return the built stream
     * @throws IllegalStateException if the builder has already transitioned to
     *         the built state
     */
    public T build() {
        if (!isBuilt.compareAndSet(false, true)) {
            throwBuilt();
        }

        T stream = build0();
        builders.clear();
        return stream;
    }

    protected abstract T build0();
}
