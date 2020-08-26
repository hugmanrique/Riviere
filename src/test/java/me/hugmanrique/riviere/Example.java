package me.hugmanrique.riviere;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class Example {

    static class PartyInvitationSender {

        static class Invitee {

            private final String emailAddress;

            public Invitee(final String emailAddress) {
                this.emailAddress = Objects.requireNonNull(emailAddress);
            }

            public String getEmailAddress() {
                return emailAddress;
            }

            // ...
        }

        // Stream builders are designed to avoid the copying overhead
        // that comes from using an ArrayList as a temporary buffer.

        // ConcurrentStreamBuilder implementations decrease the memory overhead
        // and increases the data locality associated with a ConcurrentLinkedQueue.
        //
        // Stream builders are designed to avoid the memory overhead

        // ConcurrentStreamBuilder implementations

        // Stream builders are designed to avoid the copying overhead
        // associated with using an ArrayList as a temporary buffer.
        // If the number of.

        // Stream builders avoid the copying overhead associated with
        // using an ArrayList as a temporary buffer. More so, there's
        // no clear concurrent equivalent.

        // Stream builders avoid the copying overhead associated with using
        // an ArrayList as a temporary buffer. More so, there's no clear
        // concurrent equivalent: ConcurrentLinkedQueue is based on
        // singly linked nodes, which increases the memory overhead
        // and reduces the data locality.
        //
        // ConcurrentStreamBuilder enjoys the best of both worlds:
        // it employs a non-blocking enqueuing algorithm and uses
        // a singly linked unrolled list with increasing node capacities
        // (similar to SpinedBuffer, used by instances returned by
        // Stream.builder() and its primitive equivalents).
        //
        // Another positive side effect of this data structure is
        // a maximum capacity of 2^32 - 1 elements, compared to an array's
        // maximum length of 2^31 - 1.
        // This shouldn't be a problem unless you're making a party for atoms.
        // https://www.quora.com/Is-32-bytes-enough-to-enumerate-all-the-atoms-in-the-universe
        private final Stream.Builder<Invitee> invitees;

        public PartyInvitationSender() {
            this.invitees = new ConcurrentStreamBuilder<>();
        }

        public void invite(final Invitee invitee) {
            // Imagine this program exposes an HTTP API to add invites.
            // Requests are handled by a thread pool, so this method
            // must behave correctly when called from multiple threads.
            //
            // The Builder instances returned by Stream.builder() are
            // not thread-safe. The builder's state variables are
            // updated in separate atomic operations, so it is
            // susceptible to lost updates.
            //
            // In contrast, ConcurrentStreamBuilder implementations
            // are thread-safe and guarantee the built Stream contains
            // all the invitees, in the order they were added.
            invitees.accept(invitee);
        }

        public void sendInvitations() {
            // build() transitions the builder to the built state.
            // Further invite attempts will throw IllegalStateException.
            invitees.build()
                .map(Invitee::getEmailAddress)
                .forEach(emailAddress -> {
                    // TODO Send invitation email
                });
        }
    }

    /*static class Person {
        private String fullName;
        private final String emailAddress;

        public Person(final String fullName, final String emailAddress) {
            this.fullName = Objects.requireNonNull(fullName);
            this.emailAddress = Objects.requireNonNull(emailAddress);
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        // ...
    }

    // @ThreadSafe
    static class PartyBuilder {

        private final Stream.Builder<Person> invitees;

        public PartyBuilder() {
            this.invitees = new ConcurrentStreamBuilder<>();
        }

        public void addInvitee(final Person person) {
            // Imagine this program exposes an HTTP API to add invitees.
            // Each request is handled by a thread from a Executor's pool,
            // so this method must be able to accept invites thread-safely.
            // This method must accept invites in a thread-safe manner.
            //
            // Calling #accept on a builder returned by Stream.builder()
            // is not thread-safe

            // Imagine invitees are added through an HTTP API

            // Imagine invitees are added through an HTTP API where
            // each request is handled by a

            // Imagine invitees are added through an HTTP API and
            // each request is handled by a worker thread.
            // Imagine this method is called from HTTP request handlers.
            invitees.accept(person);
        }

        public void sendInvitations() {
            //invitees.build().findAny()

            // Calling #build sets the builder
            // party accepts no more invitees
            invitees.build()
                // Get the email address of the invitee
                .map(Person::getEmailAddress)
                .forEach(emailAddress -> {
                    // TODO Send invite email
                });
        }
    }

    /*class Poll {

        private final Stream.Builder<String> builder = new ConcurrentStreamBuilder<>();

        public void acceptCandidate(final String fullName) {
            Objects.requireNonNull(fullName);
            builder.accept(fullName);
        }

        public void close() {

        }
    }

    class InvitationAcceptor*/


    class LogService implements Closeable {

        private final Path outputFile;
        private final Stream.Builder<String> builder;

        public LogService(final Path outputFile) {
            this.outputFile = outputFile;
            this.builder = new ConcurrentStreamBuilder<>();
        }

        void log(final String message) {
            builder.accept(message);
        }

        @Override
        public void close() throws IOException {
            // Write all messages to disk
            Stream<String> messages = builder.build();

            try (var writer = new PrintWriter(outputFile.toFile(), StandardCharsets.UTF_8)) {
                messages.forEach(writer::println);
            }
        }
    }

    /*class CollatzChecker {
        private final IntStream.Builder builder;
        private final ExecutorService executor;

        public CollatzChecker(final ExecutorService executor) {
            this.executor = Objects.requireNonNull(executor);
            this.builder = new ConcurrentIntStreamBuilder();
        }
    }*/

    void run() throws InterruptedException {
        IntStream.Builder streamBuilder = new ConcurrentIntStreamBuilder();
        ExecutorService executor = Executors.newCachedThreadPool();

        // Create 10 producer tasks with values from 1 to 9
        List<Callable<Object>> tasks = IntStream.rangeClosed(1, 10)
                // This Runnable is executed by threads from the executor's pool.
                // Calling #accept on a builder returned by IntStream.builder()
                // is not thread-safe. Builder additions are not atomic,
                // so they are susceptible to lost updates.
                // In contrast, ConcurrentStreamBuilder implementations are thread-safe.
                .mapToObj(value -> (Runnable) () -> streamBuilder.accept(value))
                // Wrap Runnable in a Callable<Object> since #invokeAll
                // takes a collection of Callables.
                .map(Executors::callable)
                .collect(Collectors.toList());

        // Execute the tasks and await for their completion
        executor.invokeAll(tasks);

        // Finally, construct the unordered stream
        IntStream stream = streamBuilder.build(); // {1, 2, ..., 9}
    }

    void sleep() {
        LongStream.Builder streamBuilder = new ConcurrentLongStreamBuilder();

        Thread first = new Thread(() -> {
            // Add the first item
            streamBuilder.accept(1L);
        });

        Thread second = new Thread(() -> {
            try {
                // Sleep for 1 second to guarantee the first thread
                // adds its item first.
                Thread.sleep(1000L);

                // Add the second item
                streamBuilder.accept(2L);

                // Construct the ordered stream
                LongStream stream = streamBuilder.build(); // 1, 2
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        first.start();
        second.start();
    }
}
