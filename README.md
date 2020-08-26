# 🏞️ Riviere

[![artifact][artifact]][artifact-url]
[![javadoc][javadoc]][javadoc-url]
[![tests][tests]][tests-url]
[![license][license]][license-url]

**Riviere** provides concurrent [`Stream`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html) builder implementations.
This implementation employs an enqueuing algorithm based on a singly linked unrolled list proposed
by B. Didot in [A Non-Blocking Concurrent Queue Algorithm](https://www.epfl.ch/labs/lamp/wp-content/uploads/2019/01/BrunoDIDOT.pdf),
with support for variable node capacities, primitive types, and `null` elements.

Under moderate to high contention, this significantly outperforms implementations based on
serialized access to a builder returned by [`Stream.builder()`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html#builder())
(and primitive equivalents) and a synchronized [`ArrayList`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/ArrayList.html)
used as a temporary buffer (which additionally introduces copying overhead).

## Installation

Requires Java 11 or later.

### Maven

```xml
<dependency>
    <groupId>me.hugmanrique</groupId>
    <artifactId>riviere</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'me.hugmanrique:riviere:1.1.0'
}
```

## Usage

Concurrent variants are provided for each stream type (`Stream<T>`, `IntStream`, `LongStream`, `DoubleStream`).
For example,

```java
@ThreadSafe
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
```

This class accepts messages from multiple threads










---

```java
IntStream.Builder streamBuilder = new ConcurrentIntStreamBuilder();
ExecutorService executor = Executors.newCachedThreadPool();

// Create 10 producer tasks with values from 1 to 10
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
IntStream stream = streamBuilder.build(); // {1, 2, ..., 9, 10}
```

You may specify the initial (the first node) capacity of the builder. It defaults to 16.
The size of subsequent nodes is unspecified, and it may change in the future.

> Doesn't [`Stream.Builder`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.Builder.html)
> say [`#build()`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.Builder.html#build())
> returns an ordered `Stream` whose elements are in the order they were added to the builder?

`ConcurrentStreamBuilder` implementations closely follow the `Stream.Builder` contract. In this case,
[`ExecutorService.invokeAll`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/concurrent/ExecutorService.html)
makes no execution ordering guarantees, resulting in different permutations. Observe the following:

```java
// Create a LongStream builder with an initial capacity of 2
LongStream.Builder streamBuilder = new ConcurrentLongStreamBuilder(2);

Thread first = new Thread(() -> {
    // Add the first item
    streamBuilder.accept(1L);
});

Thread second = new Thread(() -> {
    try {
        // Sleep for 1 second to (almost) guarantee the first thread adds its item first.
        Thread.sleep(1000L);

        // Add the second item
        streamBuilder.accept(2L);

        // Construct the ordered stream
        LongStream stream = streamBuilder.build(); // 1, 2 (*)
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

first.start();
second.start();
```

A careful analysis reveals no [_happens-before_](https://docs.oracle.com/javase/tutorial/essential/concurrency/memconsist.html)
relationship exists between the `Builder.accept` operations. The JMM ensures `first` is started
before `second`, but the JVM is free to reorder the operations taken by each thread.
`ConcurrentStreamBuilder` adds the appropriate synchronization to prohibit this.

**\*** This example relies on `Thread.sleep` timing being precise and accurate enough.
It is possible for the `second` thread to execute `Builder.build` before the `first` thread
has added the `1` element. The resultant stream would only contain the `2` element
and the `Builder.accept` call would throw `IllegalStateException`.
This is extremely unlikely, but you shouldn't depend on `Thread.sleep` to prevent data races.

Check out the [javadoc][javadoc-url] for more in-depth documentation.
Please feel free to create an issue if you need additional help.

## License

[MIT](LICENSE) &copy; [Hugo Manrique](https://hugmanrique.me)

[artifact]: https://img.shields.io/maven-central/v/me.hugmanrique/riviere
[artifact-url]: https://search.maven.org/artifact/me.hugmanrique/riviere
[javadoc]: https://javadoc.io/badge2/me.hugmanrique/riviere/javadoc.svg
[javadoc-url]: https://javadoc.io/doc/me.hugmanrique/riviere
[tests]: https://img.shields.io/travis/hugmanrique/Riviere/main.svg
[tests-url]: https://travis-ci.org/hugmanrique/Riviere
[license]: https://img.shields.io/github/license/hugmanrique/Riviere.svg
[license-url]: LICENSE
