package me.hugmanrique.riviere;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * A {@link LongStream.Builder} supporting full concurrency of additions.
 */
public final class ConcurrentLongStreamBuilder
        extends AbstractConcurrentStreamBuilder<long[], LongSupplier>
        implements LongStream.Builder {

    private static final VarHandle ITEM = MethodHandles.arrayElementVarHandle(long[].class);

    private static final class LongNode extends Node<long[], LongSupplier> {

        private LongNode(final int capacity) {
            super(capacity);
        }

        private LongNode(final int capacity, final long firstItem) {
            super(capacity, 1);
            // Piggyback on publication via CAS
            ITEM.set(this.items, 0, firstItem);
        }

        @Override
        protected long[] newArray(final int length) {
            return new long[length];
        }

        @Override
        protected void setVolatile(final int index, final LongSupplier supplier) {
            ITEM.setVolatile(this.items, index, supplier.getAsLong());
        }
    }

    /**
     * Constructs a concurrent {@link LongStream} builder.
     */
    public ConcurrentLongStreamBuilder() {}

    /**
     * Constructs a concurrent {@link LongStream} builder with
     * the given initial node capacity.
     *
     * @param initialCapacity the capacity of the head node
     */
    public ConcurrentLongStreamBuilder(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected LongNode createEmptyNode(final int capacity) {
        return new LongNode(capacity);
    }

    @Override
    protected LongNode createNextNode(final int capacity, final LongSupplier valueSupplier) {
        return new LongNode(capacity, valueSupplier.getAsLong());
    }

    @Override
    public void accept(final long value) {
        enqueue(() -> value);
    }

    @Override
    public LongStream build() {
        checkAndSetBuilt();
        return StreamSupport.longStream(new BuilderSpliterator(), false);
    }

    private final class BuilderSpliterator extends AbstractSpliterator<Spliterator.OfLong>
            implements Spliterator.OfLong {

        @Override
        public boolean tryAdvance(final LongConsumer action) {
            Objects.requireNonNull(action);
            boolean canAdvance = advance();
            if (canAdvance) {
                action.accept(node.items[index]);
            }
            return canAdvance;
        }
    }
}
