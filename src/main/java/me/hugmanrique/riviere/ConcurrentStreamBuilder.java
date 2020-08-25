package me.hugmanrique.riviere;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link Stream.Builder} supporting full concurrency of additions.
 *
 * @param <T> the type of the stream elements
 */
public final class ConcurrentStreamBuilder<T> extends AbstractConcurrentStreamBuilder<T[], Supplier<T>>
        implements Stream.Builder<T> {

    private static final VarHandle ITEM = MethodHandles.arrayElementVarHandle(Object[].class);

    private final class TNode extends Node<T[], Supplier<T>> {

        private TNode(final int capacity) {
            super(capacity);
        }

        private TNode(final int capacity, final T firstItem) {
            super(capacity, 1);
            // Piggyback on publication via CAS
            ITEM.set(this.items, 0, firstItem);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T[] newArray(final int length) {
            return (T[]) new Object[length];
        }

        @Override
        protected void setVolatile(final int index, final Supplier<T> supplier) {
            ITEM.setVolatile(this.items, index, supplier.get());
        }
    }

    /**
     * Constructs a concurrent {@link Stream} builder.
     */
    public ConcurrentStreamBuilder() {}

    /**
     * Constructs a concurrent {@link Stream} builder with
     * the given initial node capacity.
     *
     * @param initialCapacity the capacity of the head node
     */
    public ConcurrentStreamBuilder(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected TNode createEmptyNode(final int capacity) {
        return new TNode(capacity);
    }

    @Override
    protected TNode createNextNode(final int capacity, final Supplier<T> valueSupplier) {
        return new TNode(capacity, valueSupplier.get());
    }

    @Override
    public void accept(final T value) {
        enqueue(() -> value);
    }

    @Override
    public Stream<T> build() {
        checkAndSetBuilt();
        return StreamSupport.stream(new BuilderSpliterator(), false);
    }

    private final class BuilderSpliterator extends AbstractSpliterator<Spliterator<T>>
            implements Spliterator<T> {

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            Objects.requireNonNull(action);
            boolean canAdvance = advance();
            if (canAdvance) {
                action.accept(node.items[index]);
            }
            return canAdvance;
        }
    }
}
