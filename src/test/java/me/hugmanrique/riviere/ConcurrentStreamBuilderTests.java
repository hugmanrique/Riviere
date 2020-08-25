package me.hugmanrique.riviere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ConcurrentStreamBuilderTests {

    static final Car RED_CAR = new Car("red");
    static final Car BLUE_CAR = new Car("blue");
    static final Car GREEN_CAR = new Car("green");

    @Test
    void testConstruction() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.accept(RED_CAR);
        assertEquals(builder, builder.add(BLUE_CAR));
    }

    @Test
    void testBuild() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.add(RED_CAR).add(GREEN_CAR);

        Stream<Car> stream = builder.build();
        List<Car> elements = stream.collect(Collectors.toList());

        assertEquals(RED_CAR, elements.get(0));
        assertEquals(GREEN_CAR, elements.get(1));
        assertEquals(2, elements.size());
    }

    @Test
    void testDuplicateBuildsThrow() {
        var builder = new ConcurrentStreamBuilder<Car>();
        builder.add(RED_CAR);
        builder.build();

        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.add(GREEN_CAR));
    }

    @Test
    void testEmptyBuild() {
        var builder = new ConcurrentStreamBuilder<Car>();
        var stream = builder.build();
        assertEquals(0, stream.count());

        assertThrows(IllegalStateException.class, () -> builder.add(RED_CAR));
    }

    @Test
    void testSegmentAdds() throws InterruptedException {
        final int threadCount = 8;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        var builder = new ConcurrentStreamBuilder<Car>();

        for (int i = 0; i < threadCount; i++) {
            service.execute(() -> {
                builder.add(RED_CAR);
                latch.countDown();
            });
        }

        latch.await();
        service.shutdown();
        assertEquals(threadCount, builder.build().count());
    }

    @Test
    void testOrderGuarantee() throws InterruptedException {
        var builder = new ConcurrentStreamBuilder<Car>();

        // Prevents the builder from depending on the current thread's hashCode
        // to guarantee the Stream.Builder addition ordering guarantee.
        class ThreadWithHashCode extends Thread {
            private final int hashCode;

            public ThreadWithHashCode(final int hashCode, final Runnable target) {
                super(target);
                this.hashCode = hashCode;
            }

            @Override
            public int hashCode() {
                return hashCode;
            }
        }

        // hashCodes and execution order are in reverse order
        var first = new ThreadWithHashCode(3, () -> builder.add(RED_CAR));
        var second = new ThreadWithHashCode(2, () -> builder.add(BLUE_CAR));
        var third = new ThreadWithHashCode(0, () -> builder.add(GREEN_CAR));

        first.start();
        first.join();
        second.start();
        second.join();
        third.start();
        third.join();

        List<Car> elements = builder.build().collect(Collectors.toList());

        assertEquals(RED_CAR, elements.get(0));
        assertEquals(BLUE_CAR, elements.get(1));
        assertEquals(GREEN_CAR, elements.get(2));
    }

    static class Car {
        final String color;

        public Car(final String color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return color + " car";
        }
    }
}
