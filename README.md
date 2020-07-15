# 🏞️ Riviere

[![artifact][artifact]][artifact-url]
[![javadoc][javadoc]][javadoc-url]
[![tests][tests]][tests-url]
[![license][license]][license-url]

**Riviere** provides concurrent [`Stream`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html) builder implementations.

## Installation

Requires Java 11 or later.

### Maven

```xml
<dependency>
    <groupId>me.hugmanrique</groupId>
    <artifactId>riviere</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'me.hugmanrique:riviere:1.0.0'
}
```

## Usage

Concurrent variants are provided for each stream type (`Stream<T>`, `IntStream`, `LongStream`, `DoubleStream`).
For example,

```java
var builder = new ConcurrentIntStreamBuilder();

builder.add(1);

new Thread(() -> {
    builder.add(2);
}).start();

IntStream stream = builder.build(); // 1, 2
```

You may also specify the level of expected concurrency by passing the number of maximum buckets
to the constructor. It defaults to the number of available processors.

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
