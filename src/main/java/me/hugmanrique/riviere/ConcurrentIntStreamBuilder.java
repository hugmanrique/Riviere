package me.hugmanrique.riviere;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * A {@link IntStream.Builder} supporting full concurrency of additions.
 *
 * <p>Memory consistency effects: actions in a thread prior to adding
 * an element into a {@link ConcurrentIntStreamBuilder} <i>happen-before</i>
 * actions subsequent to calling {@link #build()} in another thread.
 */
public final class ConcurrentIntStreamBuilder
        extends AbstractConcurrentStreamBuilder<int[], IntSupplier> implements IntStream.Builder {

    private static final class IntNode extends Node<int[], IntSupplier> {

        private IntNode(final int capacity) {
            super(capacity);
        }

        private IntNode(final int capacity, final int firstItem) {
            super(capacity, 1);
            this.items[0] = firstItem;
        }

        @Override
        protected int[] newArray(final int length) {
            return new int[length];
        }

        @Override
        protected void setPlain(final int index, final IntSupplier supplier) {
            this.items[index] = supplier.getAsInt();
        }
    }

    /**
     * Constructs a concurrent {@link IntStream} builder.
     */
    public ConcurrentIntStreamBuilder() {}

    /**
     * Constructs a concurrent {@link IntStream} builder with
     * the given initial node capacity.
     *
     * @param initialCapacity the capacity of the head node
     */
    public ConcurrentIntStreamBuilder(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected IntNode createEmptyNode(final int capacity) {
        return new IntNode(capacity);
    }

    @Override
    protected IntNode createNextNode(final int capacity, final IntSupplier valueSupplier) {
        return new IntNode(capacity, valueSupplier.getAsInt());
    }

    @Override
    public void accept(final int value) {
        enqueue(() -> value);
    }

    @Override
    public IntStream build() {
        checkAndSetBuilt();
        return StreamSupport.intStream(new BuilderSpliterator(), false);
    }

    private final class BuilderSpliterator extends AbstractSpliterator<Spliterator.OfInt>
            implements Spliterator.OfInt {

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            Objects.requireNonNull(action);
            boolean advance = canAdvance();
            if (advance) {
                action.accept(node.items[index++]);
            }
            return advance;
        }
    }
}
