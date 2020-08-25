package me.hugmanrique.riviere;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

/**
 * A {@link DoubleStream.Builder} supporting full concurrency of additions.
 *
 * <p>Memory consistency effects: actions in a thread prior to adding
 * an element into a {@link ConcurrentDoubleStreamBuilder} <i>happen-before</i>
 * actions subsequent to calling {@link #build()} in another thread.
 */
public final class ConcurrentDoubleStreamBuilder
        extends AbstractConcurrentStreamBuilder<double[], DoubleSupplier>
        implements DoubleStream.Builder {

    private static final class DoubleNode extends Node<double[], DoubleSupplier> {

        private DoubleNode(final int capacity) {
            super(capacity);
        }

        private DoubleNode(final int capacity, final double firstItem) {
            super(capacity, 1);
            this.items[0] = firstItem;
        }

        @Override
        protected double[] newArray(final int length) {
            return new double[length];
        }

        @Override
        protected void setPlain(final int index, final DoubleSupplier supplier) {
            this.items[index] = supplier.getAsDouble();
        }
    }

    /**
     * Constructs a concurrent {@link DoubleStream} builder.
     */
    public ConcurrentDoubleStreamBuilder() {}

    /**
     * Constructs a concurrent {@link DoubleStream} builder with
     * the given initial node capacity.
     *
     * @param initialCapacity the capacity of the head node
     */
    public ConcurrentDoubleStreamBuilder(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected DoubleNode createEmptyNode(final int capacity) {
        return new DoubleNode(capacity);
    }

    @Override
    protected DoubleNode createNextNode(final int capacity, final DoubleSupplier valueSupplier) {
        return new DoubleNode(capacity, valueSupplier.getAsDouble());
    }

    @Override
    public void accept(final double value) {
        enqueue(() -> value);
    }

    @Override
    public DoubleStream build() {
        checkAndSetBuilt();
        return StreamSupport.doubleStream(new BuilderSpliterator(), false);
    }

    private final class BuilderSpliterator extends AbstractSpliterator<Spliterator.OfDouble>
            implements Spliterator.OfDouble {

        @Override
        public boolean tryAdvance(final DoubleConsumer action) {
            Objects.requireNonNull(action);
            boolean advance = canAdvance();
            if (advance) {
                action.accept(node.items[index++]);
            }
            return advance;
        }
    }
}
