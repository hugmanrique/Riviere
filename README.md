# 🏞️ Riviere

[![tests][tests]][tests-url]
[![license][license]][license-url]

**Riviere** provides concurrent [`Stream`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html) builder implementations.

## Installation

Riviere requires Java 11 or later.

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
    compile 'me.hugmanrique:riviere:1.0.0-SNAPSHOT'
}
```

## Usage

Concurrent variants are provided for each stream type (`Stream<T>`, `IntStream`, `LongStream`, `DoubleStream`). For example,

```java
var builder = new ConcurrentIntStreamBuilder();

builder.add(1);

new Thread(() -> {
    builder.add(2);
}).start();

IntStream stream = builder.build(); // 1, 2
```

Check out the [javadoc]() for more in-depth documentation.
Please feel free to create an issue if you need additional help.

## License

[MIT](LICENSE) &copy; [Hugo Manrique](https://hugmanrique.me)

[tests]: https://img.shields.io/travis/hugmanrique/TaskGroup/master.svg
[tests-url]: https://travis-ci.org/hugmanrique/TaskGroup
[license]: https://img.shields.io/github/license/hugmanrique/TaskGroup.svg
[license-url]: LICENSE
