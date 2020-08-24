package me.hugmanrique.riviere.newv;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public final class ConcurrentIntStreamBuilder
        extends AbstractConcurrentStreamBuilder<Integer, int[], IntSupplier>
        implements IntStream.Builder {

    private static final VarHandle ITEM = MethodHandles.arrayElementVarHandle(int[].class);

    private static final class IntNode extends Node<int[], IntSupplier> {

        public IntNode(final int capacity) {
            super(capacity);
        }

        public IntNode(final int capacity, final int firstItem) {
            super(capacity, 1);
            // Piggyback on publication via CAS
            ITEM.set(this.items, 0, firstItem);
        }

        @Override
        protected int[] newArray(final int length) {
            return new int[length];
        }

        @Override
        protected void setVolatile(final int index, final IntSupplier supplier) {
            ITEM.setVolatile(this.items, index, supplier.getAsInt());
        }
    }

    public ConcurrentIntStreamBuilder() {
    }

    public ConcurrentIntStreamBuilder(final int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    protected IntNode createEmptyNode(final int capacity) {
        return new IntNode(capacity);
    }

    @Override
    protected IntNode createNextNode(final int capacity,
                                                      final IntSupplier valueSupplier) {
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

    private final class BuilderSpliterator extends AbstractSpliterator
            implements Spliterator.OfInt {

        @Override
        public OfInt trySplit() {
            return null;
        }

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            Objects.requireNonNull(action);
            boolean canAdvance = advance();
            if (canAdvance) {
                action.accept(node.items[index]);
            }
            return canAdvance;
        }
    }
}
