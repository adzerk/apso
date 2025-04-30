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

## [0.22.1] - 2025-04-30

This release reintroduces the `config.Cache.implementation` method in `apso-caching`, now typed in both the cache key
and value and returning a `Scaffeine` instance. This should make lower-level cache usages easier.

### Added
- Add `implementation` method to `apso-caching` config class ([#833](https://github.com/adzerk/apso/pull/833)).

### Fixed
- Fix non-determinism in CachedFunctionExtrasSpec failure eviction ([#832](https://github.com/adzerk/apso/pull/832)).

[0.22.1]: https://github.com/adzerk/apso/compare/v0.22.0...v0.22.1

## [0.22.0] - 2025-04-23

This release includes full support for Scala 3, now that `apso-caching` is ported. `apso-caching`'s API has breaking
changes; check the [README](README.md) for the updated documentation.

In addition, the performance of the `flattenedKeyValueSet` in `apso-circe` was improved.

### Added
- Port `apso-caching` to Scaffeine ([#821](https://github.com/adzerk/apso/pull/821)).
- Start cross-compiling apso-caching to Scala 3 ([#827](https://github.com/adzerk/apso/pull/827)).

### Changed
- Update sbt-mdoc to 2.7.0 ([#822](https://github.com/adzerk/apso/pull/822)).
- Use builders and avoid Sets in flattenedKeyValueSet ([#825](https://github.com/adzerk/apso/pull/825)).
- Avoid specifying a default value for the maximum cache size ([#826](https://github.com/adzerk/apso/pull/826)).
- Update sbt-mdoc to 2.7.1 ([#828](https://github.com/adzerk/apso/pull/828)).
- Update unirest-java-core to 4.4.6 ([#829](https://github.com/adzerk/apso/pull/829)).
- Update circe-core, circe-generic, ... to 0.14.13 ([#830](https://github.com/adzerk/apso/pull/830)).

### Removed
- Remove LruCache ([#818](https://github.com/adzerk/apso/pull/818)).
- Remove `scala-collection-compat` dependency listing ([#824](https://github.com/adzerk/apso/pull/824)).

[0.22.0]: https://github.com/adzerk/apso/compare/v0.21.1...v0.22.0

## [0.21.1] - 2025-04-09

This is a patch release towards 0.22.0, in which we expect to have full support for Scala 3. We have started
cross-publishing most modules to Scala 3. Currently, only apso-caching is not yet available in Scala 3.

In this release we have dropped the apso-elasticsearch module. It's usefulness was debatable since it was pinned to a
specific Elasticsearch version.

We have also improved the implementation of the `list` method of `S3FileDescriptor`, making it much faster in
those scenarios where a bucket contains multiple files.

### Added
- Start cross-compiling most modules to Scala 3 ([#807](https://github.com/adzerk/apso/pull/807)).

### Changed
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.778 ([#758](https://github.com/adzerk/apso/pull/758)).
- Update log4j-core, log4j-slf4j-impl to 2.24.2 ([#760](https://github.com/adzerk/apso/pull/760)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.779 ([#761](https://github.com/adzerk/apso/pull/761)).
- Update sbt, scripted-plugin to 1.10.6 ([#762](https://github.com/adzerk/apso/pull/762)).
- Update sbt-pgp to 2.3.1 ([#763](https://github.com/adzerk/apso/pull/763)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.780 ([#764](https://github.com/adzerk/apso/pull/764)).
- Update log4j-core, log4j-slf4j-impl to 2.24.3 ([#765](https://github.com/adzerk/apso/pull/765)).
- Update sbt-mdoc to 2.6.2 ([#766](https://github.com/adzerk/apso/pull/766)).
- Update sbt, scripted-plugin to 1.10.7 ([#767](https://github.com/adzerk/apso/pull/767)).
- Install sbt in GitHub actions ([#768](https://github.com/adzerk/apso/pull/768)).
- Update commons-codec to 1.17.2 ([#769](https://github.com/adzerk/apso/pull/769)).
- Update pekko-actor, ... to 1.1.3 ([#770](https://github.com/adzerk/apso/pull/770)).
- Update nscala-time to 3.0.0 ([#771](https://github.com/adzerk/apso/pull/771)).
- Update sbt-scalafmt to 2.5.3 ([#772](https://github.com/adzerk/apso/pull/772)).
- Update scalafmt-core to 3.8.4 ([#773](https://github.com/adzerk/apso/pull/773)).
- Update sbt-scalafix to 0.14.0 ([#774](https://github.com/adzerk/apso/pull/774)).
- Update bcpkix-jdk18on, bcprov-jdk18on to 1.80 ([#775](https://github.com/adzerk/apso/pull/775)).
- Update scala-library to 2.13.16 ([#776](https://github.com/adzerk/apso/pull/776)).
- Update sbt-scalafmt to 2.5.4 ([#777](https://github.com/adzerk/apso/pull/777)).
- Update scalafmt-core to 3.8.5 ([#778](https://github.com/adzerk/apso/pull/778)).
- Update cats-core to 2.13.0 ([#779](https://github.com/adzerk/apso/pull/779)).
- Update scalafmt-core to 3.8.6 ([#780](https://github.com/adzerk/apso/pull/780)).
- Update commons-codec to 1.18.0 ([#782](https://github.com/adzerk/apso/pull/782)).
- Update joda-time to 2.13.1 ([#783](https://github.com/adzerk/apso/pull/783)).
- Update scala-collection-compat to 2.13.0 ([#784](https://github.com/adzerk/apso/pull/784)).
- Update sbt-mdoc to 2.6.3 ([#785](https://github.com/adzerk/apso/pull/785)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.781 ([#786](https://github.com/adzerk/apso/pull/786)).
- Update sbt-mdoc to 2.6.4 ([#787](https://github.com/adzerk/apso/pull/787)).
- Update scalafmt-core to 3.9.0 ([#788](https://github.com/adzerk/apso/pull/788)).
- Update SCM connection string ([#789](https://github.com/adzerk/apso/pull/789)).
- Update sbt-scalafix to 0.14.2 ([#790](https://github.com/adzerk/apso/pull/790)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.782 ([#791](https://github.com/adzerk/apso/pull/791)).
- Update scalafmt-core to 3.9.1 ([#792](https://github.com/adzerk/apso/pull/792)).
- Update scalafmt-core to 3.9.2 ([#793](https://github.com/adzerk/apso/pull/793)).
- Update shapeless to 2.3.13 ([#794](https://github.com/adzerk/apso/pull/794)).
- Update sbt, scripted-plugin to 1.10.8 ([#795](https://github.com/adzerk/apso/pull/795)).
- Update sbt, scripted-plugin to 1.10.10 ([#796](https://github.com/adzerk/apso/pull/796)).
- Update scalafmt-core to 3.9.3 ([#797](https://github.com/adzerk/apso/pull/797)).
- Update scalafmt-core to 3.9.4 ([#798](https://github.com/adzerk/apso/pull/798)).
- Update circe-core, circe-generic, ... to 0.14.12 ([#799](https://github.com/adzerk/apso/pull/799)).
- Update sbt, scripted-plugin to 1.10.11 ([#800](https://github.com/adzerk/apso/pull/800)).
- Update sbt-mdoc to 2.6.5 ([#801](https://github.com/adzerk/apso/pull/801)).
- Update specs2-common, specs2-core, ... to 4.21.0 ([#802](https://github.com/adzerk/apso/pull/802)).
- Have a single build.sbt file ([#803](https://github.com/adzerk/apso/pull/803)).
- Move elasticsearch-pekko to elasticsearch ([#806](https://github.com/adzerk/apso/pull/806)).
- Update joda-time to 2.14.0 ([#817](https://github.com/adzerk/apso/pull/817)).

### Removed
- Remove Guava option from apso-caching ([#810](https://github.com/adzerk/apso/pull/810)).
- Remove apso-elasticsearch ([#811](https://github.com/adzerk/apso/pull/811)).
- Drop support for Scala 2.12 ([#813](https://github.com/adzerk/apso/pull/813)).
- Remove custom `Timestamp` implementation from `caching` ([#814](https://github.com/adzerk/apso/pull/814)).

### Fixed
- Make fromFullPaths tail recursive ([#759](https://github.com/adzerk/apso/pull/759)).
- Fix CI badge ([#781](https://github.com/adzerk/apso/pull/781)).
- Remove Scala Steward pins related to Elasticsearch dependencies ([#816](https://github.com/adzerk/apso/pull/816)).
- Avoid fetching all prefixes when listing S3 files under a directory ([#819](https://github.com/adzerk/apso/pull/819)).

[0.21.1]: https://github.com/adzerk/apso/compare/v0.21.0...v0.21.1

## [0.21.0] - 2024-11-06

In this release we have added a `uri` method to the `FileDescriptor` data type. The `uri` method can be used to produce
a `java.net.URI` from a `FileDescriptor`.

```scala
import com.kevel.apso.io._

val localFD = FileDescriptor("file:///tmp/one/two/three")
localFD.uri
// Returns java.net.URI = file:///tmp/one/two/three

val s3FD = FileDescriptor("s3://my-bucket/key")
s3FD.uri
// Returns java.net.URI = s3://my-bucket/key
```

### Added
- Add a `uri` method to `FileDescriptor` ([#756](https://github.com/adzerk/apso/pull/756)).

### Changed
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.776 ([#749](https://github.com/adzerk/apso/pull/749)).
- Update sbt, scripted-plugin to 1.10.3 ([#750](https://github.com/adzerk/apso/pull/750)).
- Update aws-java-sdk-s3 to 1.12.777 ([#751](https://github.com/adzerk/apso/pull/751)).
- Update sbt, scripted-plugin to 1.10.4 ([#752](https://github.com/adzerk/apso/pull/752)).
- Update bcpkix-jdk18on, bcprov-jdk18on to 1.79 ([#753](https://github.com/adzerk/apso/pull/753)).
- Update unirest-java-core to 4.4.5 ([#754](https://github.com/adzerk/apso/pull/754)).
- Update sbt, scripted-plugin to 1.10.5 ([#755](https://github.com/adzerk/apso/pull/755)).

[0.21.0]: https://github.com/adzerk/apso/compare/v0.20.0...v0.21.0

## [0.20.0] - 2024-10-16

This release is an exact copy of v0.19.7, but now published under the `com.kevel` namespace.

[0.20.0]: https://github.com/adzerk/apso/compare/v0.19.7...v0.20.0

## [0.19.7] - 2024-10-16

This release includes some bug fixes and dependency updates. It will also be last version of Apso published under the
`com.velocidi` namespace. Future releases will start using the `com.kevel` namespace.

### Added
- Force empty line after top-level package statement ([#727](https://github.com/adzerk/apso/pull/727)).

### Changed
- Update shapeless to 2.3.12 ([#680](https://github.com/adzerk/apso/pull/680)).
- Update aws-java-sdk-core to 1.12.726 ([#681](https://github.com/adzerk/apso/pull/681)).
- Update aws-java-sdk-core to 1.12.731 ([#683](https://github.com/adzerk/apso/pull/683)).
- Update cats-core to 2.12.0 ([#684](https://github.com/adzerk/apso/pull/684)).
- Update aws-java-sdk-s3 to 1.12.733 ([#685](https://github.com/adzerk/apso/pull/685)).
- Update aws-java-sdk-s3 to 1.12.739 ([#687](https://github.com/adzerk/apso/pull/687)).
- Update specs2-common, specs2-core, ... to 4.20.7 ([#688](https://github.com/adzerk/apso/pull/688)).
- Update aws-java-sdk-core to 1.12.742 ([#689](https://github.com/adzerk/apso/pull/689)).
- Update aws-java-sdk-s3 to 1.12.744 ([#690](https://github.com/adzerk/apso/pull/690)).
- Update scalafmt-core to 3.8.2 ([#691](https://github.com/adzerk/apso/pull/691)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.748 ([#696](https://github.com/adzerk/apso/pull/696)).
- Update circe-core, circe-generic, ... to 0.14.8 ([#697](https://github.com/adzerk/apso/pull/697)).
- Update sbt-mdoc to 2.5.3 ([#698](https://github.com/adzerk/apso/pull/698)).
- Update scalatest-core to 3.2.19 ([#699](https://github.com/adzerk/apso/pull/699)).
- Update aws-java-sdk-s3 to 1.12.750 ([#700](https://github.com/adzerk/apso/pull/700)).
- Update pekko-actor, ... to 1.0.3 ([#701](https://github.com/adzerk/apso/pull/701)).
- Update sbt-sonatype to 3.11.0 ([#702](https://github.com/adzerk/apso/pull/702)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.756 ([#703](https://github.com/adzerk/apso/pull/703)).
- Update circe-core, circe-generic, ... to 0.14.9 ([#704](https://github.com/adzerk/apso/pull/704)).
- Update aws-java-sdk-s3 to 1.12.759 ([#705](https://github.com/adzerk/apso/pull/705)).
- Update sbt to 1.10.1 ([#706](https://github.com/adzerk/apso/pull/706)).
- Update sbt-mdoc to 2.5.4 ([#707](https://github.com/adzerk/apso/pull/707)).
- Update specs2-common, specs2-core, ... to 4.20.8 ([#708](https://github.com/adzerk/apso/pull/708)).
- Update aws-java-sdk-core to 1.12.761 ([#709](https://github.com/adzerk/apso/pull/709)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.762 ([#710](https://github.com/adzerk/apso/pull/710)).
- Update commons-codec to 1.17.1 ([#711](https://github.com/adzerk/apso/pull/711)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.765 ([#712](https://github.com/adzerk/apso/pull/712)).
- Update unirest-java-core to 4.4.4 ([#713](https://github.com/adzerk/apso/pull/713)).
- Update scalafmt-core to 3.8.3 ([#714](https://github.com/adzerk/apso/pull/714)).
- Update sbt-sonatype to 3.11.1 ([#715](https://github.com/adzerk/apso/pull/715)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.767 ([#716](https://github.com/adzerk/apso/pull/716)).
- Update sbt-sonatype to 3.11.2 ([#717](https://github.com/adzerk/apso/pull/717)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.768 ([#718](https://github.com/adzerk/apso/pull/718)).
- Update aws-java-sdk-s3 to 1.12.769 ([#719](https://github.com/adzerk/apso/pull/719)).
- Update sbt-sonatype to 3.11.3 ([#720](https://github.com/adzerk/apso/pull/720)).
- Update aws-java-sdk-core to 1.12.770 ([#721](https://github.com/adzerk/apso/pull/721)).
- Update pekko-actor, ... to 1.1.0 ([#722](https://github.com/adzerk/apso/pull/722)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.771 ([#723](https://github.com/adzerk/apso/pull/723)).
- Update scala-library to 2.12.20 ([#724](https://github.com/adzerk/apso/pull/724)).
- Update circe-core, circe-generic, ... to 0.14.10 ([#725](https://github.com/adzerk/apso/pull/725)).
- Update log4j-core, log4j-slf4j-impl to 2.24.0 ([#726](https://github.com/adzerk/apso/pull/726)).
- Update aws-java-sdk-s3 to 1.12.772 ([#728](https://github.com/adzerk/apso/pull/728)).
- Update sshj to 0.39.0 ([#729](https://github.com/adzerk/apso/pull/729)).
- Update pekko-actor, ... to 1.1.1 ([#730](https://github.com/adzerk/apso/pull/730)).
- Update joda-time to 2.13.0 ([#731](https://github.com/adzerk/apso/pull/731)).
- Update sbt to 1.10.2 ([#732](https://github.com/adzerk/apso/pull/732)).
- Update scalacheck to 1.18.1 ([#733](https://github.com/adzerk/apso/pull/733)).
- Update sbt-mdoc to 2.6.0 ([#734](https://github.com/adzerk/apso/pull/734)).
- Update sbt-mdoc to 2.6.1 ([#735](https://github.com/adzerk/apso/pull/735)).
- Update scala-library to 2.13.15 ([#736](https://github.com/adzerk/apso/pull/736)).
- Update sbt-scalafix to 0.13.0 ([#737](https://github.com/adzerk/apso/pull/737)).
- Update nscala-time to 2.34.0 ([#738](https://github.com/adzerk/apso/pull/738)).
- Update log4j-core, log4j-slf4j-impl to 2.24.1 ([#739](https://github.com/adzerk/apso/pull/739)).
- Update pekko-http, pekko-http-core, ... to 1.1.0 ([#740](https://github.com/adzerk/apso/pull/740)).
- Update aws-java-sdk-core, aws-java-sdk-s3 to 1.12.773 ([#741](https://github.com/adzerk/apso/pull/741)).
- Update sbt-pgp to 2.3.0 ([#742](https://github.com/adzerk/apso/pull/742)).
- Update pekko-actor-testkit-typed to 1.1.2 ([#743](https://github.com/adzerk/apso/pull/743)).
- Update sbt-sonatype to 3.12.0 ([#744](https://github.com/adzerk/apso/pull/744)).
- Update sbt-sonatype to 3.12.2 ([#746](https://github.com/adzerk/apso/pull/746)).
- Update specs2-common, specs2-core, ... to 4.20.9 ([#747](https://github.com/adzerk/apso/pull/747)).

### Removed
- Remove the coursier/cache-action step ([#695](https://github.com/adzerk/apso/pull/695)).

### Fixed
- Fix race condition in LruCache when handling exceptions ([#692](https://github.com/adzerk/apso/pull/692)).
- Fix `ValueMagnet` implicit conversion from `Any` ([#693](https://github.com/adzerk/apso/pull/693)).
- Fix release date of 0.19.6 in the Changelog ([#694](https://github.com/adzerk/apso/pull/694)).

[0.19.7]: https://github.com/adzerk/apso/compare/v0.19.6...v0.19.7

## [0.19.6] - 2024-05-17

This release introduces a new configuration key, `aws.s3.max-connections`, for the maximum allowed connections of the S3
client.

### Added
- Add `aws.s3.max-connections` setting for S3 client ([#677](https://github.com/adzerk/apso/pull/677)).

[0.19.6]: https://github.com/adzerk/apso/compare/v0.19.5...v0.19.6

## [0.19.5] - 2024-05-02

This is a maintenance release focused on the update of [Bouncy Castle's](https://www.bouncycastle.org/) libraries
provided in apso-io, dealing with CVE-2024-29857, CVE-2024-30171, CVE-2024-30172 and SNYK-JAVA-ORGBOUNCYCASTLE-6612984.

### Security
- Override `bcprov-jdk18on` transient dependency ([#663](https://github.com/adzerk/apso/pull/663)).

[0.19.5]: https://github.com/adzerk/apso/compare/v0.19.4...v0.19.5

## [0.19.4] - 2024-04-29

This is a maintenance release focused on dependency updates and security fixes. Most notably, this release updates the
version of jackson-databind, which fixes CVE-2022-42004, CVE-2022-42003 and CVE-2020-36518.

### Changed
- Minor dependency updates.

### Security
- Force eviction of jackson-databind 2.13.1 ([#660](https://github.com/adzerk/apso/pull/660)).

[0.19.4]: https://github.com/adzerk/apso/compare/v0.19.3...v0.19.4

## [0.19.3] - 2024-02-26

### Added
- Add `utcLocalDateTime` to `ApsoTimeDateTime` ([#628](https://github.com/adzerk/apso/pull/628)).

### Changed
- Minor dependency updates.

[0.19.3]: https://github.com/adzerk/apso/compare/v0.19.2...v0.19.3

## [0.19.2] - 2024-02-20

This release focuses on the addition of new Pekko modules, along with multiple performance improvements.

It also fixes some issues with the retry logic when calling S3, where some errors were not being retried.

### Added
- Add Pekko subprojects ([#616](https://github.com/adzerk/apso/pull/616)).

### Changed
- Ensure that all classes are compiled with Java 11 bytecode ([#621](https://github.com/adzerk/apso/pull/621)).
- Minor dependency updates.

### Fixed
- Fix compilation for Java 19+ ([#611](https://github.com/adzerk/apso/pull/611)).
- Small improvement to JsonConvert performance ([#612](https://github.com/adzerk/apso/pull/612)).
- Improve retry logic with S3 client ([#613](https://github.com/adzerk/apso/pull/613).
- Mark Retry sleep as blocking ([#614](https://github.com/adzerk/apso/pull/614)).
- Improvement to JsonConvert performance 2.0 ([#615](https://github.com/adzerk/apso/pull/615)).
- Performance improvements converting Java Map to Json ([#622](https://github.com/adzerk/apso/pull/622)).

[0.19.2]: https://github.com/adzerk/apso/compare/v0.19.1...v0.19.2

## [0.19.1] - 2023-11-30

This release focuses on starting to use Java 11 and updating dependencies.
The most notable dependency update is Unirest, to version 4.2.0, which solves concurrency issues when using `W`.

### Changed
- Update Unirest to version 4.2.0 ([#583](https://github.com/adzerk/apso/pull/583)).
- Start using Java 11 ([#584](https://github.com/adzerk/apso/pull/584)).
- Update other dependencies.

[0.19.1]: https://github.com/adzerk/apso/compare/v0.19.0...v0.19.1

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

[0.19.0]: https://github.com/adzerk/apso/compare/v0.18.8...v0.19.0

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

[0.18.8]: https://github.com/adzerk/apso/compare/v0.18.7...v0.18.8

## [0.18.7] - 2023-02-28

### Changed
- Update elasticsearch-related dependencies to `7.16.x` ([#432](https://github.com/adzerk/apso/pull/432)).

[0.18.7]: https://github.com/adzerk/apso/compare/v0.18.6...v0.18.7

## [0.18.6] - 2023-02-22

### Changed
- Update elasticsearch-related dependencies to the most recent version within the `7.x.x` major version ([#427](https://github.com/adzerk/apso/pull/427)).
- Other dependency updates.

[0.18.6]: https://github.com/adzerk/apso/compare/v0.18.5...v0.18.6

## [0.18.4] - 2022-05-20

### Changed
- Disable some Elasticsearch features on ElasticsearchTestKit ([#262](https://github.com/adzerk/apso/pull/262)).
- Allow Elasticsearch base path to be overridden on ElasticsearchTestKit ([#263](https://github.com/adzerk/apso/pull/263)).
- Dependency updates.

[0.18.4]: https://github.com/adzerk/apso/compare/v0.18.3...v0.18.4

## [0.18.3] - 2022-05-11

This is a maintenance release, with only dependency updates.

[0.18.3]: https://github.com/adzerk/apso/compare/v0.18.2...v0.18.3

## [0.18.2] - 2022-02-24

### Removed
- Remove Log4j dependency from apso-log ([#153](https://github.com/adzerk/apso/pull/153)).
- Remove TryWith in favor of scala 2.13 native resource management tools ([#187](https://github.com/adzerk/apso/pull/187)).

[0.18.2]: https://github.com/adzerk/apso/compare/v0.18.1...v0.18.2

## [0.18.1] - 2021-07-06

This is the first version with a Changelog, albeit not being the first version of Apso.

### Changed
- Update Scala, dependencies and plugins versions ([#142](https://github.com/adzerk/apso/pull/142)).

[0.18.1]: https://github.com/adzerk/apso/compare/v0.18.0...v0.18.1

***

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
