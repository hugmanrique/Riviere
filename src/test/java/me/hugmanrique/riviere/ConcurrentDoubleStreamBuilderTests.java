package me.hugmanrique.riviere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import org.junit.jupiter.api.Test;

public class ConcurrentDoubleStreamBuilderTests {

    @Test
    void testAccepts() {
        var builder = new ConcurrentDoubleStreamBuilder();
        builder.accept(1);
        assertEquals(builder, builder.add(2));
    }

    @Test
    void testBuild() {
        var builder = new ConcurrentDoubleStreamBuilder();
        builder.accept(1);
        builder.accept(2);
        builder.accept(0);
        builder.accept(1);

        double[] elements = builder.build().toArray();
        assertArrayEquals(new double[] { 1, 2, 0, 1 }, elements);
    }

    @Test
    void testEmptyBuild() {
        var builder = new ConcurrentDoubleStreamBuilder();
        DoubleStream stream = builder.build();
        assertEquals(0, stream.count());
    }

    @Test
    void testBuiltStateChecks() {
        var builder = new ConcurrentDoubleStreamBuilder();
        builder.accept(2);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.accept(2));
    }

    @Test
    void testAcceptsWithContention() throws InterruptedException {
        var builder = new ConcurrentDoubleStreamBuilder();
        int expectedCount = TestUtils.withContention(() -> builder.add(1));
        double[] elements = builder.build().toArray();
        assertEquals(expectedCount, elements.length);
        for (int i = 0; i < expectedCount; i++)
            assertEquals(1, elements[i]);
    }

    @Test
    void testAcceptsOrdering() throws InterruptedException {
        var builder = new ConcurrentDoubleStreamBuilder();
        double[] expected = ThreadLocalRandom.current().doubles(100).toArray();
        Queue<Runnable> tasks = Arrays.stream(expected)
                .mapToObj(value -> (Runnable) () -> builder.add(value))
                .collect(Collectors.toCollection(ArrayDeque::new));
        TestUtils.testOrdering(tasks);
        double[] elements = builder.build().toArray();
        assertArrayEquals(expected, elements);
    }
}
