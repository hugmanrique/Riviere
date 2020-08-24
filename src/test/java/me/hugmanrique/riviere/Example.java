package me.hugmanrique.riviere;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Example {

    /*void run() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        IntStream.Builder streamBuilder = new ConcurrentIntStreamBuilder();

        class IntProducer implements Runnable {
            final int value;

            IntProducer(final int value) {
                this.value = value;
            }

            @Override
            public void run() {
                // This method is called by threads from the executor's pool.
                // Calling #accept on a builder returned by IntStream.builder()
                // is not thread-safe. Builder additions are not atomic, so
                // they are susceptible to lost updates.
                // In contrast, ConcurrentStreamBuilder implementations are thread-safe.
                streamBuilder.accept(this.value);
            }
        }

        // Create 10 producer tasks with values from 1 to 9
        List<Callable<Object>> tasks = IntStream.range(1, 10)
                .mapToObj(IntProducer::new)
                // Wrap Runnable in a Callable<Object> since
                // #invokeAll takes a collection of Callables.
                .map(Executors::callable)
                .collect(Collectors.toList());

        // Execute the tasks and await for their completion
        executor.invokeAll(tasks);

        // Finally, construct the unordered stream
        IntStream stream = streamBuilder.build(); // {1, 2, ..., 9}
    }*/
}
