package me.hugmanrique.riviere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

public class ConcurrentLongStreamBuilderTests {

    @Test
    void testAccepts() {
        var builder = new ConcurrentLongStreamBuilder();
        builder.accept(1);
        assertEquals(builder, builder.add(2));
    }

    @Test
    void testBuild() {
        var builder = new ConcurrentLongStreamBuilder();
        builder.accept(1);
        builder.accept(2);
        builder.accept(0);
        builder.accept(1);

        long[] elements = builder.build().toArray();
        assertArrayEquals(new long[] { 1, 2, 0, 1 }, elements);
    }

    @Test
    void testEmptyBuild() {
        var builder = new ConcurrentLongStreamBuilder();
        LongStream stream = builder.build();
        assertEquals(0, stream.count());
    }

    @Test
    void testBuiltStateChecks() {
        var builder = new ConcurrentLongStreamBuilder();
        builder.accept(2);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.accept(2));
    }

    @Test
    void testAcceptsWithContention() throws InterruptedException {
        var builder = new ConcurrentLongStreamBuilder();
        int expectedCount = TestUtils.withContention(() -> builder.add(1));
        long[] elements = builder.build().toArray();
        assertEquals(expectedCount, elements.length);
        for (int i = 0; i < expectedCount; i++)
            assertEquals(1, elements[i]);
    }

    @Test
    void testAcceptsOrdering() throws InterruptedException {
        var builder = new ConcurrentLongStreamBuilder();
        Queue<Runnable> tasks = LongStream.range(1, 100)
                .mapToObj(value -> (Runnable) () -> builder.add(value))
                .collect(Collectors.toCollection(ArrayDeque::new));
        TestUtils.testOrdering(tasks);
        long[] elements = builder.build().toArray();
        assertArrayEquals(LongStream.range(1, 100).toArray(), elements);
    }
}
