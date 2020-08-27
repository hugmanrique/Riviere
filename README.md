# 🏞️ Riviere

[![artifact][artifact]][artifact-url]
[![javadoc][javadoc]][javadoc-url]
[![tests][tests]][tests-url]
[![license][license]][license-url]

**Riviere** provides concurrent [`Stream`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html) builder implementations.

Stream builders avoid the copying overhead associated with using an [`ArrayList`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/ArrayList.html)
as a temporary buffer. However, there's no clear concurrent equivalent:
[`ConcurrentLinkedQueue`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/concurrent/ConcurrentLinkedQueue.html)
is based on singly linked nodes, which increases the memory overhead and reduces data locality.<br/>
[`ConcurrentStreamBuilder`](https://javadoc.io/doc/me.hugmanrique/riviere/latest/me/hugmanrique/riviere/ConcurrentStreamBuilder.html)
enjoys the best of both worlds: it employs a non-blocking enqueuing algorithm based on
a singly linked unrolled list proposed in [A Non-Blocking Concurrent Queue Algorithm](https://www.epfl.ch/labs/lamp/wp-content/uploads/2019/01/BrunoDIDOT.pdf)
by B. Didot, with support for increasing node capacities (similar to `SpinedBuffer`, used by
instances returned by `Stream.builder()`), primitive types,
and `null` elements.

Under moderate to high contention, this library significantly outperforms implementations based on
serialized access to a builder returned by [`Stream.builder()`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html#builder()).

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

Let's suppose we have a program that exposes an HTTP API to manage invitations to a party.
Requests are handled by a thread pool, so `PartyInvitationSender` must behave correctly when
adding invites from different threads.

The [`Builder`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.Builder.html)
instances returned by [`Stream.builder()`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html#builder())
are not thread-safe. The builder's state variables are updated in separate atomic actions, so
it is susceptible to lost updates.

In contrast, [`ConcurrentStreamBuilder`](https://javadoc.io/doc/me.hugmanrique/riviere/latest/me/hugmanrique/riviere/ConcurrentStreamBuilder.html)
implementations are thread-safe and guarantee the built [`Stream`](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/util/stream/Stream.html)
contains all the invites, in the order they were added.

```java
public class PartyInvitationSender {

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

    private final Stream.Builder<Invitee> invitees;

    public PartyInvitationSender() {
        this.invitees = new ConcurrentStreamBuilder<>();
    }

    public void invite(final Invitee invitee) {
        invitees.accept(invitee);
    }

    public void sendInvitations() {
        // build() transitions the builder to the built state.
        // Further addition attempts will throw IllegalStateException.
        invitees.build()
            .map(Invitee::getEmailAddress)
            .forEach(emailAddress -> {
                // TODO Send invitation email
            });
    }
}
```

You may specify the initial (the first node) capacity of the builder. It defaults to 16.
The size of subsequent nodes is unspecified, and it may change in the future.

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
