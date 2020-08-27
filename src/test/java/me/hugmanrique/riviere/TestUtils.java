package me.hugmanrique.riviere;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class TestUtils {

    // The debugger shows Node counts regularly go over the Node capacity
    // by ~2-6 increments with these parameters.
    private static final int DEFAULT_THREAD_COUNT = 8;
    private static final int DEFAULT_PER_THREAD = 100;

    static int withContention(final Runnable runnable) throws InterruptedException {
        return withContention(runnable, DEFAULT_THREAD_COUNT, DEFAULT_PER_THREAD);
    }

    /**
     * Executes the given {@link Runnable} {@code runsPerThread} times
     * on {@code threadCount} threads.
     *
     * @param runnable the task to execute
     * @param threadCount the number of threads
     * @param runsPerThread the number of times to run the task on each thread
     * @return the number of times the task was executed
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    static int withContention(final Runnable runnable,
                               final int threadCount, final int runsPerThread)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        var latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                for (int j = 0; j < runsPerThread; j++)
                    runnable.run();
                latch.countDown();
            });
        }
        try {
            latch.await();
        } finally {
            executor.shutdown();
        }
        return threadCount * runsPerThread;
    }

    private static final long TASK_SLEEP_DELTA = 25L; // ms

    private static boolean executeBatch(final ExecutorService executor, final Queue<Runnable> tasks,
                                        final int count)
            throws InterruptedException {
        List<Callable<Void>> toSubmit = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Runnable task = tasks.poll();
            if (task == null)
                break;
            int j = i;
            toSubmit.add(() -> {
                try {
                    Thread.sleep(TASK_SLEEP_DELTA * j);
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }
        // Task executions are not ordered by happens-before, #invokeAll only
        // guarantees all task actions happens-before the next batch starts.
        executor.invokeAll(toSubmit);
        return toSubmit.size() == count;
    }

    static void testOrdering(final Queue<Runnable> tasks) throws InterruptedException {
        testOrdering(tasks, DEFAULT_THREAD_COUNT);
    }

    /**
     * Asserts the sequential execution of tasks on independent threads
     * are ordered by <i>happens-before</i> when no external ordering
     * guarantees are made.
     *
     * <p>This test relies on {@link Thread#sleep(long)} timing being precise
     * and accurate enough. However, this may yield false positives or
     * false negatives sporadically.
     *
     * @param tasks the tasks to run in order (approximate)
     * @param threadCount the number of task-executor threads
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    static void testOrdering(final Queue<Runnable> tasks, final int threadCount)
            throws InterruptedException {
        if (threadCount >= tasks.size())
            throw new IllegalArgumentException("Thread count must be lower than task count");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            //noinspection StatementWithEmptyBody
            while (executeBatch(executor, tasks, threadCount)) {}
        } finally {
            executor.shutdown();
        }
    }

    private TestUtils() {
        throw new AssertionError();
    }
}
