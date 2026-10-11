# Statistica

Statistica sends a plugin's usage statistics to [bStats](https://bstats.org), [FastStats](https://faststats.dev)
or both, and reports its errors to FastStats. A plugin declares its metrics once; Statistica turns them
into each service's charts, keeps counters exact across both, lets only one instance of a cluster send
shared totals, and hides player data from error reports. Java 17 or newer is required, Java 21 on Velocity.

## Installation

```kotlin
repositories {
    maven("https://registry.whereareiam.me/maven/packages")
    maven("https://repo.faststats.dev/releases")
}

dependencies {
    // The platform module, with the capability of each service the plugin sends to:
    implementation("me.whereareiam:statistica-platform-velocity:1.0.0") {
        capabilities { requireCapability("me.whereareiam:statistica-platform-velocity-bstats") }
    }
    implementation("me.whereareiam:statistica-platform-velocity:1.0.0") {
        capabilities { requireCapability("me.whereareiam:statistica-platform-velocity-faststats") }
    }

    // Only to elect one instance of a cluster, see below:
    implementation("me.whereareiam:statistica-adapter-jdbc:1.0.0")   // through a shared database
    implementation("me.whereareiam:statistica-adapter-redis:1.0.0")  // through a shared Redis
}
```

| Module | For |
| --- | --- |
| `statistica-platform-velocity` | Velocity plugins |
| `statistica-platform-bungeecord` | BungeeCord and Waterfall plugins |
| `statistica-adapter-jdbc` | electing through a database, with its table as a Strata stream |
| `statistica-adapter-redis` | electing through Redis, with the plugin's Jedis |

A capability pulls in that service's library; without it the service's code is never loaded. Shade and
relocate what you ship: bStats refuses to run unrelocated, and relocating `dev.faststats` keeps two plugins'
FastStats apart. Keep FastStats' `META-INF/faststats.properties` in the jar and leave Gson unrelocated, the
proxies provide it.

## Send metrics

```java
Counter logins = new Counter();

Statistica statistica = VelocityStatistica.builder(this, server, logger, dataDirectory)
        .bstats(metricsFactory, 12345)                       // Velocity injects bStats' Metrics.Factory
        .faststats("0123456789abcdef0123456789abcdef")       // project token, ships with the plugin
        .metrics(metrics -> metrics
                .string("storage", () -> settings.getStorage().name())
                .stringList("modules", modules::enabledIds)
                .number("logins", logins)
                .total("accounts", accounts::cachedCount))
        .build();

statistica.start();     // when the plugin is ready
logins.increment();     // anywhere, cheap on a hot path
statistica.stop();      // when the plugin shuts down
```

On BungeeCord the entry point is `BungeeCordStatistica.builder(plugin)` with `.bstats(12345)`.

| Declared as | bStats chart | FastStats value |
| --- | --- | --- |
| `string` | simple pie | `string` |
| `stringList` | advanced pie, each value counted once | `stringArray` |
| `number` | single line chart | `number` |
| `numberMap` | advanced pie | `numberMap` |
| `groupedMap` | drilldown pie | `numberMap` with `group:key` keys |
| `total`, `totalMap` | as `number`, `numberMap` | as `number`, `numberMap` |

Create each chart on bStats and each data source on FastStats under the metric's id before it shows up.

Values are read on the services' own threads every 30 minutes. Keep them cheap and send only fixed values
the plugin controls, such as enum names and its own ids, never anything a server owner or player typed.

### Counters

A `Counter` or `CounterMap` counts events between submissions. Each service sends what was counted since
its own last successful submission: a failed FastStats submission is carried over, and neither service can
reset what the other has not sent yet. bStats does not confirm delivery, so a failed bStats submission loses
its events rather than sending them twice.

### Shared totals and elections

Instances of a plugin that share their data, such as proxies on one database, would each send the same
totals. Metrics declared with `total` or `totalMap` are only sent by the elected instance:

```java
JdbcElection election = JdbcElection.of(dataSource, "myplugin_statistica_lease");

new Strata(List.of(pluginDatabase, election.schema("my-plugin/statistica"))).migrate();

builder.election(election);
```

The election is a lease in one row: the holder renews it, gives it up when it stops, and another instance
takes it over when it expires. Elect through the store the totals come from. Without an election every
instance is elected, which is right for one that shares nothing.

## Report errors

```java
try {
    step.run(connection);
} catch (RuntimeException exception) {
    statistica.errors().report(exception, ErrorContext.builder()
            .attribute("step", step.id())
            .redact(connection.username())
            .build());
}
```

Errors go to FastStats and are dropped when only bStats is configured. A report:

- is only sent when the plugin's own code threw it: its class loader, and those added with
  `ErrorPolicy.Builder#owner`, defined the class that threw. Errors of other plugins and third-party
  extensions stay on the server.
- hides the values passed to `redact` wherever they appear in the messages, together with unique ids,
  e-mail addresses, and the values databases and Jdbi put into their error messages. FastStats itself hides
  IP addresses, home directories and database passwords.
- is dropped when its type or message is ignored, and once 100 reports were kept in 30 minutes.

```java
builder.errors(errors -> errors
        .ignore(ConnectException.class)     // a setup problem, not a bug
        .attribute("storage", settings.getStorage().name()));
```

Errors of the plugin's code that no handler caught are captured as well, unless `captureUncaught(false)`.

## Server owners

Both services ask server owners first and honour their choice; Statistica adds nothing to opt out of.

| Service | File | Off |
| --- | --- | --- |
| bStats | `plugins/bStats/config.txt` | `enabled=false` |
| FastStats | `plugins/faststats/config.properties` | `enabled=false`; `submitErrors=false` for errors only |

FastStats sends nothing on the first start, when it creates its file.
