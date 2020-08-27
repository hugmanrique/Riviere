package me.hugmanrique.riviere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ConcurrentIntStreamBuilderTests {

    @Test
    void testAccepts() {
        var builder = new ConcurrentIntStreamBuilder();
        builder.accept(1);
        assertEquals(builder, builder.add(2));
    }

    @Test
    void testBuild() {
        var builder = new ConcurrentIntStreamBuilder();
        builder.accept(1);
        builder.accept(2);
        builder.accept(0);
        builder.accept(1);

        int[] elements = builder.build().toArray();
        assertArrayEquals(new int[] { 1, 2, 0, 1 }, elements);
    }

    @Test
    void testEmptyBuild() {
        var builder = new ConcurrentIntStreamBuilder();
        IntStream stream = builder.build();
        assertEquals(0, stream.count());
    }

    @Test
    void testBuiltStateChecks() {
        var builder = new ConcurrentIntStreamBuilder();
        builder.accept(2);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.accept(2));
    }

    @Test
    void testAcceptsWithContention() throws InterruptedException {
        var builder = new ConcurrentIntStreamBuilder();
        int expectedCount = TestUtils.withContention(() -> builder.add(1));
        int[] elements = builder.build().toArray();
        assertEquals(expectedCount, elements.length);
        for (int i = 0; i < expectedCount; i++)
            assertEquals(1, elements[i]);
    }

    @Test
    void testAcceptsOrdering() throws InterruptedException {
        var builder = new ConcurrentIntStreamBuilder();
        Queue<Runnable> tasks = IntStream.range(1, 100)
                .mapToObj(value -> (Runnable) () -> builder.add(value))
                .collect(Collectors.toCollection(ArrayDeque::new));
        TestUtils.testOrdering(tasks);
        int[] elements = builder.build().toArray();
        assertArrayEquals(IntStream.range(1, 100).toArray(), elements);
    }
}
