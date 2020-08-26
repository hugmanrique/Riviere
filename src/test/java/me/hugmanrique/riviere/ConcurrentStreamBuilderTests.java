package me.hugmanrique.riviere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ConcurrentStreamBuilderTests {

    static final Car RED_CAR = new Car("red");
    static final Car GREEN_CAR = new Car("green");
    static final Car BLUE_CAR = new Car("blue");

    @Test
    void testAccepts() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.accept(RED_CAR);
        assertEquals(builder, builder.add(GREEN_CAR));
    }

    @Test
    void testBuild() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.accept(RED_CAR);
        builder.accept(GREEN_CAR);
        builder.accept(BLUE_CAR);
        builder.accept(RED_CAR);

        Car[] elements = builder.build().toArray(Car[]::new);
        assertArrayEquals(new Car[] { RED_CAR, GREEN_CAR, BLUE_CAR, RED_CAR }, elements);
    }

    @Test
    void testEmptyBuild() {
        var builder = new ConcurrentStreamBuilder<Car>();
        Stream<Car> stream = builder.build();
        assertEquals(0, stream.count());
    }

    @Test
    void testNullElements() {
        var builder = new ConcurrentStreamBuilder<Car>();
        // At the back
        Car[] expected = new Car[] { RED_CAR, null, null };
        builder.add(RED_CAR).add(null).add(null);
        Car[] elements = builder.build().toArray(Car[]::new);
        assertArrayEquals(expected, elements);

        // At the front
        builder = new ConcurrentStreamBuilder<>();
        expected = new Car[] { null, null, GREEN_CAR };
        builder.add(null).add(null).add(GREEN_CAR);
        elements = builder.build().toArray(Car[]::new);
        assertArrayEquals(expected, elements);

        // In between
        builder = new ConcurrentStreamBuilder<>();
        expected = new Car[] { RED_CAR, null, BLUE_CAR };
        builder.add(RED_CAR).add(null).add(BLUE_CAR);
        elements = builder.build().toArray(Car[]::new);
        assertArrayEquals(expected, elements);
    }

    @Test
    void testBuiltStateChecks() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.accept(RED_CAR);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.accept(GREEN_CAR));
    }

    @Test
    void testAcceptsWithContention() throws InterruptedException {
        var builder = new ConcurrentStreamBuilder<Car>();
        int expectedCount = TestUtils.withContention(() -> builder.add(RED_CAR));
        Car[] elements = builder.build().toArray(Car[]::new);
        assertEquals(expectedCount, elements.length);
        for (int i = 0; i < expectedCount; i++)
            assertEquals(RED_CAR, elements[i]);
    }

    @Test
    void testAcceptsOrdering() throws InterruptedException {
        var builder = new ConcurrentStreamBuilder<Car>();
        Car[] expected = IntStream.range(1, 100)
                .mapToObj(value -> new Car(Integer.toString(value)))
                .toArray(Car[]::new);
        Queue<Runnable> tasks = Arrays.stream(expected)
                .map(car -> (Runnable) () -> builder.add(car))
                .collect(Collectors.toCollection(ArrayDeque::new));
        TestUtils.testOrdering(tasks);
        Car[] elements = builder.build().toArray(Car[]::new);
        assertArrayEquals(expected, elements);
    }

    private static class Car {
        private final String color;

        private Car(final String color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return color + " car";
        }
    }
}
