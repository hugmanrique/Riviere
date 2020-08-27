package me.hugmanrique.riviere;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link Stream.Builder} supporting full concurrency of additions.
 * Unlike most concurrent collection implementations, this class
 * permits the use of {@code null} elements.
 *
 * <p>Memory consistency effects: actions in a thread prior to placing
 * an object into a {@link ConcurrentStreamBuilder} <i>happen-before</i>
 * actions subsequent to building the stream in another thread.
 *
 * @param <T> the type of the stream elements
 */
public final class ConcurrentStreamBuilder<T> extends AbstractConcurrentStreamBuilder<T[], Supplier<T>>
        implements Stream.Builder<T> {

    private final class TNode extends Node<T[], Supplier<T>> {

        private TNode(final int capacity) {
            super(capacity);
        }

        private TNode(final int capacity, final T firstItem) {
            super(capacity, 1);
            this.items[0] = firstItem;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T[] newArray(final int length) {
            return (T[]) new Object[length];
        }

        @Override
        protected void setPlain(final int index, final Supplier<T> supplier) {
            this.items[index] = supplier.get();
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
            boolean advance = canAdvance();
            if (advance) {
                action.accept(node.items[index++]);
            }
            return advance;
        }
    }
}
