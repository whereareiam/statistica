# AGENTS.md

Statistica sends a plugin's usage statistics to bStats, FastStats or both, and reports its errors to
FastStats. Plugins declare their metrics once; each service gets its own form of them.

## Modules

- `statistica-api`: what plugins use. Metrics, counters, the election contract, error policies and
  reports. Public API: every change is compatibility-sensitive and needs complete Javadocs.
- `statistica-common`: the lifecycle, the shared builder, cached elections, error filtering and
  redaction.
- `statistica-integration/*`: one module per service, turning metrics into its form.
- `statistica-adapter/*`: elections through a technology, a database or Redis.
- `statistica-platform/*`: one module per proxy; each service is an optional Gradle feature
  (`bstats`, `faststats`) that brings its integration and library.

Keep a service's or platform's library out of every module but its own. A platform module reaches a
service's library through a separate class only, so a plugin without that library never loads it.

## Build and test

- `./gradlew test build` builds everything and runs the fast tests.
- `-PintegrationTests=true` also runs the tests that start containers; they need Docker.
- `VERSION=<version> ./gradlew publishToMavenLocal` lets a plugin build against a local version.

## Pull requests and releases

Build each change in its own `feature/...` branch and merge it into `dev` through a pull request.
Publishing a release draft tags `dev`, publishes the Maven artifacts and attaches the jars.

### Release notes

A merged pull request lands in the release draft as `* <title> ([#<number>](<url>))`, under the
category of its label. Readers are plugin developers.

- Titles use `Area: Description`, with a past-tense description of what changed for a plugin:
  `Introduced`, `Changed`, `Fixed`, `Removed`.
- Areas: `Metrics`, `Errors`, `Elections`, `bStats`, `FastStats`, `Velocity`, `BungeeCord`. Add an
  area only for a new service, platform or adapter, and add it to this list in the same pull request.

| Label | Use for |
| --- | --- |
| `feature` | something new for plugins |
| `change` | changed behavior or API |
| `bug` | a fix for a bug that shipped in a release |
| `dependencies` | a library update |
| `skip-changelog` | refactors, build, CI, tests and documentation, and fixes of bugs that never shipped |

Add `major` next to the category when plugins must change their code on upgrade.

Text written by hand, the subtitle, the summary and upgrade notes, lives in `.github/release-notes.md`;
it replaces the draft's placeholders while its `version` matches the drafted version. The release
workflow stops while a placeholder is left.
