package me.hugmanrique.riviere;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

/**
 * A {@link DoubleStream.Builder} supporting full concurrency of additions.
 */
public final class ConcurrentDoubleStreamBuilder
        extends AbstractConcurrentStreamBuilder<double[], DoubleSupplier>
        implements DoubleStream.Builder {

    private static final VarHandle ITEM = MethodHandles.arrayElementVarHandle(double[].class);

    private static final class DoubleNode extends Node<double[], DoubleSupplier> {

        private DoubleNode(final int capacity) {
            super(capacity);
        }

        private DoubleNode(final int capacity, final double firstItem) {
            super(capacity, 1);
            // Piggyback on publication via CAS
            ITEM.set(this.items, 0, firstItem);
        }

        @Override
        protected double[] newArray(final int length) {
            return new double[length];
        }

        @Override
        protected void setVolatile(final int index, final DoubleSupplier supplier) {
            ITEM.setVolatile(this.items, index, supplier.getAsDouble());
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
            boolean canAdvance = advance();
            if (canAdvance) {
                action.accept(node.items[index]);
            }
            return canAdvance;
        }
    }
}
