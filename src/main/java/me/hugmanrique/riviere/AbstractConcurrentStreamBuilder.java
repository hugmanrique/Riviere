package me.hugmanrique.riviere;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Spliterator;
import java.util.stream.Stream;

/**
 * Concurrent {@code Stream} builder base implementation.
 *
 * <p>This implementation employs a <em>non-blocking</em> algorithm based on
 * one described in <a href="https://www.epfl.ch/labs/lamp/wp-content/uploads/2019/01/BrunoDIDOT.pdf">
 * A Non-Blocking Concurrent Queue Algorithm</a> by B. Didot, with support
 * for increasing node capacities and primitive types.
 *
 * <p>This significantly outperforms implementations based on
 * serialized access to a builder returned by {@link Stream#builder()}
 * (and primitive equivalents) and a synchronized {@link ArrayList}
 * used as a temporary buffer (which additionally introduces
 * copying overhead).
 *
 * <p>Memory consistency effects: actions in a thread prior to placing
 * an object into a {@link ConcurrentStreamBuilder} <i>happen-before</i>
 * actions subsequent to building the stream in another thread.
 *
 * @param <A> the array type for the stream element type
 * @param <S> the supplier type for the stream element type
 */
abstract class AbstractConcurrentStreamBuilder<A, S> {

    // This is a modification of the enqueuing algorithm based on
    // a singly linked unrolled list proposed by B. Didot
    // with support for variable node capacities.
    //
    // The fundamental invariants are:
    // - There is exactly one (last) Node with a null next reference,
    //   which is CASed when enqueuing. It can always be reached from
    //   the head.
    // - The items stored in array indices lower than the Node's
    //   current count are elements of the builder.
    // - A Node is full if its count is greater than or equal to
    //   its capacity.
    // - All predecessors of an initialized element are initialized.
    // - Nodes cannot be dequeued. The list can only be traversed if
    //   the builder is in the built state.
    //
    // The last point is particularly important: Node items are written
    // in plain mode. The built variable is read in opaque mode when
    // adding elements to the builder. When calling #build, a stronger
    // volatile read ensures all writes are visible when traversing
    // the list.
    // addHint is replaced by a thread-safe counter incremented atomically
    // on every write attempt.
    //
    // The tail is permitted to lag. Both head and tail may point to
    // an empty Node. Since no dequeuing is performed, tail cannot lag
    // behind head, so head need not be a dummy node as in
    // the original implementation.

    /**
     * The default initial capacity of the first {@link Node} in
     * a {@link AbstractConcurrentStreamBuilder}.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity of a {@link Node}.
     */
    // Prevents overflow by a single left-shift when creating the next Node
    private static final int MAX_NODE_CAPACITY = 1 << 30;

    abstract static class Node<A, S> {
        private volatile Node<A, S> next;
        protected final A items;
        private final int capacity;

        /**
         * The number of initialized items (or greater if the Node is
         * full, i.e. {@code count >= capacity}).
         *
         * <p>The original implementation supports weak updates to
         * an {@code addHint} counter used to speed up the last non-null
         * element search. However, Node must support primitive arrays
         * with no {@code null} equivalent (a magic constant is not
         * a valid solution). Instead, {@code count} is atomically
         * incremented every time an item addition is attempted.
         */
        private volatile int count;

        /**
         * Constructs an empty node that can hold {@code capacity} items.
         *
         * @param capacity the node capacity
         */
        Node(final int capacity) {
            if (capacity <= 0)
                throw new IllegalArgumentException("Got non-positive capacity " + capacity);
            this.capacity = capacity;
            this.items = newArray(capacity);
        }

        /**
         * Constructs a node that can hold {@code capacity} items
         * whose {@code expectedCount} first items are to be
         * initialized by the parent class constructor.
         *
         * @param capacity the node capacity
         * @param expectedCount the number of initialized items
         */
        protected Node(final int capacity, final int expectedCount) {
            this(capacity);
            // Relaxed write, we piggyback on publication via CAS
            COUNT.set(this, expectedCount);
        }

        protected abstract A newArray(int length);

        protected abstract void setPlain(final int index, final S supplier);
    }

    /**
     * Indicates the builder is in built state, at which point no
     * new elements may be enqueued and the list can be traversed.
     */
    @SuppressWarnings("UnusedVariable")
    private volatile boolean built;

    /**
     * The first node in the list (may contain items).
     */
    private final Node<A, S> head;

    /**
     * A node from which the last node on the list can be reached.
     */
    private volatile Node<A, S> tail;

    protected AbstractConcurrentStreamBuilder() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    protected AbstractConcurrentStreamBuilder(final int initialCapacity) {
        head = tail = createEmptyNode(initialCapacity);
    }

    protected abstract Node<A, S> createEmptyNode(final int capacity);

    protected abstract Node<A, S> createNextNode(final int capacity, final S valueSupplier);

    private void throwBuilt() {
        throw new IllegalStateException("Builder is in built state");
    }

    private boolean isBuilt() {
        // Don't impose any ordering constraints w.r.t. other variables
        return (boolean) BUILT.getOpaque(this);
    }

    protected void checkAndSetBuilt() {
        if (!BUILT.compareAndSet(this, false, true))
            throwBuilt();
    }

    protected void enqueue(final S valueSupplier) {
        Node<A, S> nextNode = null;
        while (!isBuilt()) {
            Node<A, S> curTail = tail;
            Node<A, S> tailNext = curTail.next;
            if (tailNext == null) {
                // curTail is last node
                int index = (int) COUNT.getAndAdd(curTail, 1);
                if (index < curTail.capacity) {
                    curTail.setPlain(index, valueSupplier);
                    return;
                } else {
                    // The node is full (was already full or lost CAS race).
                    // Create next node (if necessary) and try to append.
                    int nextCap = Math.min(curTail.capacity << 1, MAX_NODE_CAPACITY);
                    if (nextNode == null || nextNode.capacity != nextCap) {
                        nextNode = createNextNode(nextCap, valueSupplier);
                    }

                    if (NEXT.compareAndSet(curTail, null, nextNode)) {
                        // If this CAS fails, another caller will advance tail
                        TAIL.weakCompareAndSet(this, curTail, nextNode);
                        return;
                    }
                }
            } else {
                // Help advance tail
                TAIL.compareAndSet(this, curTail, tailNext);
            }
        }
        throwBuilt();
    }

    protected abstract class AbstractSpliterator<T extends Spliterator<?>> {
        protected Node<A, S> node;
        protected int index; // current index in node

        private long est = -1; // size estimate, -1 until needed

        protected AbstractSpliterator() {
            if (!isBuilt())
                throw new AssertionError(
                        "Spliterator constructed while builder is not in built state");
            this.node = head;
        }

        /**
         * Returns whether there is a remaining element.
         * The caller is responsible for incrementing the current index.
         *
         * @return {@code true} if a remaining element exists
         */
        protected boolean canAdvance() {
            if (node == null) return false;
            if (index >= node.capacity) {
                node = node.next;
                index = 0;
                if (node == null) return false;
            }
            return index < node.count;
        }

        public T trySplit() {
            return null;
        }

        public long estimateSize() {
            if (est < 0) {
                // Traverse list to sum up counts
                // TODO Nodes have increasing power of 2 capacities, we could
                // compute the size of all nodes except the last with a shift.
                long count = 0;
                Node<A, S> current = head;
                do {
                    count += Math.min(current.count, current.capacity);
                } while ((current = current.next) != null);
                est = count;
            }
            return est;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED
                    // Once the builder is in built state, no further modifications can be made
                    | Spliterator.IMMUTABLE
                    // trySplit always returns null
                    | Spliterator.SUBSIZED;
        }
    }

    // VarHandle mechanics
    private static final VarHandle TAIL;
    private static final VarHandle BUILT;
    private static final VarHandle NEXT;
    private static final VarHandle COUNT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            TAIL = l.findVarHandle(AbstractConcurrentStreamBuilder.class, "tail", Node.class);
            BUILT = l.findVarHandle(
                    AbstractConcurrentStreamBuilder.class, "built", boolean.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            COUNT = l.findVarHandle(Node.class, "count", int.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
