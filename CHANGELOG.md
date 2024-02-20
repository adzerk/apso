# Changelog

<!--
Use the following schema when setting up the Changelog for a new release. Remove empty sections before publishing.

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security
-->

## [0.19.2] - 2024-02-20

This releases focuses on the addition of new Pekko modules, along with multiple performance improvements.

It also fixes some issues with the retry logic when calling S3, where some errors were not being retried.

### Added
- Add Pekko subprojects ([#616](https://github.com/adzerk/apso/pull/616)).

### Changed
- Ensure that all classes are compiled with Java 11 bytecode ([#621](https://github.com/adzerk/apso/pull/621)).
- Minor dependency updates.

### Fixed
- Fix compilation for Java 19+ ([#611](https://github.com/adzerk/apso/pull/611)).
- Small improvement to JsonConvert performance ([#612](https://github.com/adzerk/apso/pull/611)).
- Improve retry logic with S3 client ([#613](https://github.com/adzerk/apso/pull/613).
- Mark Retry sleep as blocking ([#614](https://github.com/adzerk/apso/pull/614)).
- Improvement to JsonConvert performance 2.0 ([#615](https://github.com/adzerk/apso/pull/615)).
- Performance improvements converting Java Map to Json ([#622](https://github.com/adzerk/apso/pull/622)).

[0.19.2]: https://github.com/velocidi/apso/compare/v0.19.1...v0.19.2

## [0.19.1] - 2023-11-30

This release focuses on starting to use Java 11 and updating dependencies.
The most notable dependency update is Unirest, to version 4.2.0, which solves concurrency issues when using `W`.

### Changed
- Update Unirest to version 4.2.0 ([#583](https://github.com/adzerk/apso/pull/583)).
- Start using Java 11 ([#584](https://github.com/adzerk/apso/pull/584)).
- Update other dependencies.

[0.19.1]: https://github.com/velocidi/apso/compare/v0.19.0...v0.19.1

## [0.19.0] - 2023-10-07

This release focus mostly on updating dependencies and cleaning up code around apso-elasticsearch project.

Most notably, we deprecated `CompositeIterator` and improved method signature typing as part of our
[scala version update](https://github.com/adzerk/apso/pull/488). We also dropped `ElasticsearchTestkit` and the
apso-log project. Together, these modifications should allow users of this library to have less dependency update
conflicts and provide easier update paths for Scala 3 while we wait for a release of Apso in Scala 3.
`ElasticsearchBulkInserter` was modified to rely on `ActorLogging` instead of `scala-logging` (through apso-log) which
represented a blocker for Scala 3 projects cross-compiling with Scala 2.13 and having apso-elasticsearch as a
dependency.

Users of apso-log should migrate to [scala-logging](https://github.com/lightbend-labs/scala-logging) which provides
`LazyLogging` and `StrictLogging` traits providing the same functionality of `Logging` and `StrictLogging` in Apso,
respectively, while being available for Scala 2.13 and 3.

Several dependencies were update to their latest versions. You can see all the dependencies and their versions in
[here](https://github.com/adzerk/apso/blob/v0.19.0/project/Dependencies.scala).

### Changed
- Stop depending on `scala-logging` for logging purposes inside `ElasticsearchBulkInserter` and use  `ActorLogging` instead ([#550](https://github.com/adzerk/apso/pull/550)).
- Update Scala version to 2.13.12 ([#540](https://github.com/adzerk/apso/pull/540)).
- Update other dependencies.

### Deprecated
- Deprecate `CompositeIterator` ([#488](https://github.com/adzerk/apso/pull/488)).

### Removed
- Remove Elasticsearch TestKit ([#490](https://github.com/adzerk/apso/pull/490)).
- Drop apso-log project ([#549](https://github.com/adzerk/apso/pull/549)).

[0.19.0]: https://github.com/velocidi/apso/compare/v0.18.8...v0.19.0

## [0.18.8] - 2023-04-27

This release includes several dependency updates, some updates to make existing APIs friendlier and a change to a
`Decoder` for extra flexibility.

We have updated the `Retry` API to make it more convenient to override the default timeout in between sleeps. We
previously had to do something like the following:

```scala
Retry.retry(maxRetries = 5, inBetweenSleep = Some(2.seconds))(f)
```

But are now able to provide a duration directly, without wrapping it in an `Option`:

```scala
Retry.retry(maxRetries = 5, inBetweenSleep = 2.seconds)(f)
```

We have also promoted the SFTP file descriptor credentials from a tuple to a dedicated data type:

```scala
case class Credentials(host: String, port: String, auth: Either[Identity, String])
```

Additionally, the `Decoder` for [Squants](https://github.com/typelevel/squants)'s `Currency` was updated to become case
insensitive. This means that, for example, both `"usd"` and `"USD"` now decode to the same `USD` currency.

### Changed
- Update circe-core, circe-generic, circe-literal, circe-parser to 0.14.5 ([#441](https://github.com/adzerk/apso/pull/441)).
- Update simplejmx to 2.2 ([#443](https://github.com/adzerk/apso/pull/443)).
- Add `SFTP` file descriptor credentials ([#449](https://github.com/adzerk/apso/pull/449)).
- Enchance `Retry` API ([#450](https://github.com/adzerk/apso/pull/450)).
- Update joda-time to 2.12.5 ([#458](https://github.com/adzerk/apso/pull/458)).
- Update bcpkix-jdk18on, bcprov-jdk18on to 1.73 ([#463](https://github.com/adzerk/apso/pull/463)).
- Update aws-java-sdk-core to 1.12.450 ([#464](https://github.com/adzerk/apso/pull/464)).
- Make the `Decoder` for `Currency` case insensitive ([#467](https://github.com/adzerk/apso/pull/467)).
- Update aws-java-sdk-s3 to 1.12.457 ([#468](https://github.com/adzerk/apso/pull/468)).
- Update scala-collection-compat to 2.10.0 ([#469](https://github.com/adzerk/apso/pull/469)).

[0.18.8]: https://github.com/velocidi/apso/compare/v0.18.7...v0.18.8

## [0.18.7] - 2023-02-28

### Changed
- Update elasticsearch-related dependencies to `7.16.x` ([#432](https://github.com/adzerk/apso/pull/432)).

[0.18.7]: https://github.com/velocidi/apso/compare/v0.18.6...v0.18.7

## [0.18.6] - 2023-02-22

### Changed
- Update elasticsearch-related dependencies to the most recent version within the `7.x.x` major version ([#427](https://github.com/adzerk/apso/pull/427)).
- Other dependency updates.

[0.18.6]: https://github.com/velocidi/apso/compare/v0.18.5...v0.18.6

## [0.18.4] - 2022-05-20

### Changed
- Disable some Elasticsearch features on ElasticsearchTestKit ([#262](https://github.com/velocidi/apso/pull/262)).
- Allow Elasticsearch base path to be overridden on ElasticsearchTestKit ([#263](https://github.com/velocidi/apso/pull/263)).
- Dependency updates.

[0.18.4]: https://github.com/velocidi/apso/compare/v0.18.3...v0.18.4

## [0.18.3] - 2022-05-11

This is a maintenance release, with only dependency updates.

[0.18.3]: https://github.com/velocidi/apso/compare/v0.18.2...v0.18.3

## [0.18.2] - 2022-02-24

### Removed
- Remove Log4j dependency from apso-log ([#153](https://github.com/velocidi/apso/pull/153)).
- Remove TryWith in favor of scala 2.13 native resource management tools ([#187](https://github.com/velocidi/apso/pull/187)).

[0.18.2]: https://github.com/velocidi/apso/compare/v0.18.1...v0.18.2

## [0.18.1] - 2021-07-06

This is the first version with a Changelog, albeit not being the first version of Apso.

### Changed
- Update Scala, dependencies and plugins versions ([#142](https://github.com/velocidi/apso/pull/142)).

[0.18.1]: https://github.com/velocidi/apso/compare/v0.18.0...v0.18.1

***

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
