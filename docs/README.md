<p align="center"><img src="https://raw.githubusercontent.com/adzerk/apso/master/apso.png"/></p>

# Apso [![Build Status](https://github.com/adzerk/apso/workflows/CI/badge.svg?branch=master)](https://github.com/adzerk/apso/actions?query=workflow%3ACI+branch%3Amaster) [![Maven Central](https://img.shields.io/maven-central/v/com.velocidi/apso_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.velocidi/apso_2.12)

Apso is Kevel's collection of Scala utility libraries. It provides a series of useful methods.

## Installation

Apso's latest release is built against Scala 2.12 and Scala 2.13.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso" % "@VERSION@"
```

The project is divided in modules, you can instead install only a specific module.

The TestKit is available under the `apso-testkit` project. You can include it only for the `test` configuration:

```scala
libraryDependencies += "com.velocidi" %% "apso-testkit" % "@VERSION@" % "test"
```

Please take into account that the library is still in an experimental stage and the interfaces might change for subsequent releases.

## Table of Contents

- [Core](#core)
    - [Config](#config)
        - [LazyConfigFactory](#lazyconfigfactory)
    - [HTTP](#http)
    - [Geo](#geo)
    - [Implicits](#implicits)
    - [JreVersionHelper](#jreversionhelper)
    - [ProgressBar](#progressbar)
    - [Reflect](#reflect)
    - [Retry](#retry)
- [Akka HTTP/Pekko HTTP](#akka-http--pekko-http)
    - [ClientIPDirectives](#clientipdirectives)
    - [ExtraMiscDirectives](#extramiscdirectives)
    - [ProxySupport](#proxysupport)
- [Amazon Web Services](#amazon-web-services)
    - [ConfigCredentialsProvider](#configcredentialsprovider)
    - [CredentialStore](#credentialstore)
    - [S3Bucket](#s3bucket)
    - [SerializableAWSCredentials](#serializableawscredentials)
- [Caching](#caching)
- [Collections](#collections)
    - [Trie](#trie)
    - [TypedMap](#typedmap)
    - [Iterators](#iterators)
        - [CircularIterator](#circulariterator)
        - [MergedBufferedIterator](#mergedbufferediterator)
- [Encryption](#encryption)
- [Hashing](#hashing)
- [IO](#io)
    - [FileDescriptor](#filedescriptor)
    - [ResourceUtil](#resourceutil)
- [JSON](#json)
- [Profiling](#profiling)
    - [CpuSampler](#cpusampler)
    - [SimpleJmx](#simplejmx)
- [Time](#time)
- [TestKit](#testkit)

## Core

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-core" % "@VERSION@"
```

### Config

Apso provides methods to ease working with Typesafe's [config](https://github.com/typesafehub/config).

#### LazyConfigFactory

The `LazyConfigFactory` object provides static methods for creating `Config` instances in a lazy way. The lazy way refers to the variable loading process. The usual process loads variables in config files eagerly (i.e. the path needs to be defined in the same file it is refered to). The loading process provided by `LazyConfigFactory` loads and merges all configuration files and only then resolves variables. This loading process introduces a third file (beyond the default ones - `application.conf` and `reference.conf`): `overrides.conf`. This file has priority over the `application.conf` file and can be used to specify keys that should always be overriden, e.g. by environment variables.

### HTTP

Apso provides a tiny wrapper for [Dispatch](http://dispatch.databinder.net/) with synchronous operations. It's called `W`, and the following shows some sample usage:

```scala mdoc:compile-only
import com.kevel.apso.http.W

W.get("http://www.google.com/").getStatus
// res0: Int = 302

W.post("http://www.google.com/", "").getStatus
// res1: Int = 405

W.put("http://www.google.com/", "").getStatus
// res2: Int = 405

W.delete("http://www.google.com/").getStatus
// res3: Int = 405

W.head("http://www.google.com/").getStatus
// res4: Int = 302
```

The POST and PUT methods can also receive the body as `JSON` (of [circe](https://github.com/circe/circe)), which adds the `Content-type` header accordingly.

### Geo

The `Geo` object provides methods to compute distances in kilometers between two points on the planet Earth, calculated using the spherical [law of cosines](https://en.wikipedia.org/wiki/Great-circle_distance#Formulas). Coordinates are represented by a pair of `Double` for latitude and longitude.

```scala mdoc:reset
import com.kevel.apso.Geo

Geo.distance((41.1617609, -8.6024716), (41.1763745, -8.5964861))
```

You can also have the distance function curried if you are computing distances from a fixed point:

```scala mdoc:silent
val distFromOffice = Geo.distanceFrom((41.1617609, -8.6024716))
```
```scala mdoc
distFromOffice((41.1763745, -8.5964861))

distFromOffice((38.7223032, -9.1414664))
```

### Implicits

Apso provides implicit conversions from `String`, `Seq[_]`, `Map[_, _]`, `Seq[Map[_, _]]` and `AutoCloseable` to extended types that come packed with extended features.

```scala mdoc:reset
import com.kevel.apso.Implicits._

Seq(1, 3, 5).mergeSorted(Seq(2, 4))

(0 to 15).average

Map(1 -> 2, 3 -> 6).twoWayMerge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }

Map(1 -> 2, 2 -> 4, 3 -> 6).twoWayMerge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }

Map(1 -> 2, 2 -> 3).mapKeys(_ + 1)

```
```scala mdoc:silent
val rand = new scala.util.Random(1)
```
```scala mdoc
rand.choose((0 to 15).toSeq)

rand.choose((0 to 15).toSeq)

rand.choose((0 to 15).toSeq)

rand.choose((0 to 15).toSeq)

rand.chooseN((0 to 15).toSeq, 4)

rand.chooseN((0 to 15).toSeq, 4)
```

### JreVersionHelper

The JreVersionHelper object provides helper methods to check the two most significant parts of the JRE version at runtime:

```scala mdoc:compile-only
import com.kevel.apso.JreVersionHelper

JreVersionHelper.jreVersion
// res0: (Int, Int) = (1, 8)
```

### ProgressBar

The `ProgressBar` represents a widget to print a dynamic progress bar in a console.

```scala mdoc:compile-only
import com.kevel.apso.ProgressBar

val progress = ProgressBar(100)

progress.tick(1)
// 1% [>                                                     ] / [ 0.19 ] ops/s

progress.tick(2)
// 3% [=>                                                    ] - [ 0.15 ] ops/s

progress.tick(1)
// 4% [==>                                                   ] \ [ 0.12 ] ops/s

progress.tick(10)
// 14% [=======>                                              ] | [ 0.31 ] ops/s

progress.tick(20)
// 34% [==================>                                   ] / [ 0.46 ] ops/s

progress.tick(30)
// 64% [=================================>                    ] - [ 0.77 ] ops/s
```

### Reflect

The `Reflect` object contains helpers for reflection-related tasks, namely to create an instance of a given class given its fully qualified name and also to access singleton objects:

```scala
scala> import com.kevel.apso.Reflect
import com.kevel.apso.Reflect

scala> import com.kevel.apso.collection._
import com.kevel.apso.collection._

scala> Reflect.newInstance[HMap[Nothing]]("com.kevel.apso.collection.HMap")
res0: com.kevel.apso.collection.HMap[Nothing] = HMap()

scala> Reflect.companion[Reflect.type]("com.kevel.apso.Reflect")
res1: com.kevel.apso.Reflect.type = com.kevel.apso.Reflect$@3b1dbca
```

### Retry

The `Retry` object provides a method to retry methods or `Future`s a given number of times until they succeed or the specified maximum number of retries is reached:

```scala mdoc:reset
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.kevel.apso.Retry

import java.util.concurrent.atomic.AtomicInteger

val a = new AtomicInteger()

def f: Future[Int] = {
  Future {
    val value = a.getAndAdd(1)
    if (value > 5)
      value
    else {
      throw new Exception()
    }
  }
}

Await.result(Retry.retryFuture(10)(f), Duration.Inf)

var attempts = 0

def m() = {
  attempts += 1
  if (attempts > 5)
    attempts
  else
    throw new Exception()
}

Retry.retry(10)(m())
```

## Akka HTTP / Pekko HTTP

The `akka-http` module provides additional [directives](https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/index.html#directives) to be used in [akka-http](https://doc.akka.io/docs/akka-http/current/index.html).

The same features are provided for Pekko under the `pekko-http` module.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-akka-http" % "@VERSION@"
```

### ClientIPDirectives

The `ClientIPDirectives` trait exposes an `optionalRawClientIP` directive that extracts the raw IP of the client from either the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP` header, in that order of priority.

### ExtraMiscDirectives

The `ExtraMiscDirectives` trait exposes the directives `cacheControlMaxAge(maxAgeDuration)` and `optionalRefererHost` to set the cache-control header to the supplied finite duration (the minimum resolution is 1 second) to extract the referer from the HTTP request header, respectively. The `ExtraMiscDirectives` companion object exposes a `cacheControlNoCache` directive to reply with the `no-cache` option in the `Cache-Control` header.

### ProxySupport

The `ProxySupport` traits adds helper methods to proxy requests to a given uri, either directly (`proxyTo`), or with the unmatched path and query parameters of the current context (`proxyToUnmatchedPath`). In order for the client IP to be correctly propagated in `X-Forward-For` headers, the `ProxySupport` trait requires the `akka.http.server.remote-address-attribute` setting to be `on`.

## Amazon Web Services

Apso provides a group of classes to ease the interaction with the Amazon Web Services, namely S3 and EC2.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-aws" % "@VERSION@"
```

### ConfigCredentialsProvider

The `ConfigCredentialsProvider` is an `AWSCredentialsProvider` (from AWS SDK for Java) that retrieves credentials from a typesafe configuration, allowing customization of its `Config` object, as well as the access key and secret key paths:

```scala mdoc:silent
import com.kevel.apso.aws._

import com.typesafe.config._

val confProvider = ConfigCredentialsProvider(
  config = ConfigFactory.parseString("""{
    aws {
      access-key = "<access-key>"
      secret-key = "<secret-key>"
    }
  }"""),
  accessKeyPath = "aws.access-key",
  secretKeyPath = "aws.secret-key")

val credentials = confProvider.getCredentials

credentials.getAWSAccessKeyId

credentials.getAWSSecretKey
```

### CredentialStore

The `CredentialStore` object serves as an endpoint for the retrieval of AWS credentials from available configurations. It extends the chain in the `DefaultAWSCredentialsProviderChain` (from AWS SDK for Java) with the retrieval of AWS credentials through the default typesafe configuration file (typically `application.conf`).

### S3Bucket

The `S3Bucket` class wraps an instance of `AmazonS3Client` (from AWS SDK for Java) and exposes a higher level interface for pushing and pulling files to and from a bucket.

### SerializableAWSCredentials

The `SerializableAWSCredentials` class provides a serializable container for AWS credentials, extending the `AWSCredentials` class (from AWS SDK for Java).

## Caching
The `apso-caching` module provides provides utilities for caching.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-caching" % "@VERSION@"
```

Apso provides utilities to simplify the caching of method calls, with [ScalaCache](https://cb372.github.io/scalacache/) and using either `Guava` or `Caffeine` as underlying cache implementations.

These utilities are provided as `cached()` and `cachedF()` extension methods over all `FunctionN[]` types:

```scala mdoc:reset
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.concurrent.atomic.AtomicInteger

import com.kevel.apso.caching._

val x = new AtomicInteger(0)

val cachedFn = ((i: Int) => {
  val value = x.getAndAdd(i)
  value
}).cached(config.Cache.Caffeine(Some(3), None))

cachedFn(2)
cachedFn(2)
x

val y = new AtomicInteger(0)

val cachedFutFn = ((i: Int) => Future {
  val value = y.getAndAdd(i)
  value
}).cachedF(config.Cache.Guava(Some(2), None))

Await.result(cachedFutFn(3), Duration.Inf)
Await.result(cachedFutFn(3), Duration.Inf)
y
```

## Collections

The `apso-collections` module provides some helpful collections. To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-collections" % "@VERSION@"
```

### Trie

The `Trie` class is an implementation of an immutable trie. An example usage follows:

```scala mdoc:reset
import com.kevel.apso.collection._

val t = Trie[Char, Int]()

val nt = t.set("one", 1).set("two", 2).set("three", 3).set("four", 4)

nt.get("one")

nt.get("two")

nt.get("five")
```

### TypedMap

The `TypedMap` is a map that associates types with values. It can be used as follows:

```scala mdoc:reset
import com.kevel.apso.collection._

val m = TypedMap("one", 2, 3L)

m[String]

m[Int]

m[Long]

m.get[String]

m.get[Int]

m.get[Long]

m.get[Char]
```

### Iterators

Apso provides some utility iterators.

#### CircularIterator

The `CircularIterator` is an iterator that iterates over its elements in a circular way. See the following for sample usage:

```scala mdoc:reset
import com.kevel.apso.iterator.CircularIterator

val circularIterator = CircularIterator(List(1, 2, 3).iterator)

circularIterator.take(10).toList
```

#### MergedBufferedIterator

The `MergedBufferedIterator` is a collection of sorted `BufferedIterators` that allows traversing them in order, while also providing a `mergeSorted` method to merge with another sorted `BufferedIterator`. See the following for sample usage:

```scala mdoc:reset
import com.kevel.apso.iterator.MergedBufferedIterator

val it1 = MergedBufferedIterator(List(
         (0 to 3).iterator.buffered,
         (0 to 8).iterator.buffered,
         (0 to 15).iterator.buffered,
         (0 to 11).iterator.buffered))

it1.toList

val it2 = MergedBufferedIterator(List(
         Iterator(1, 3, 5).buffered,
         Iterator(2).buffered))

it2.mergeSorted(Iterator(4, 6).buffered).toList
```

## Encryption

Apso provides some simple utility classes to deal with encryption and decryption of data, and methods that ease the
creation of the underlying Cyphers.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-encryption" % "@VERSION@"
```

The following shows the creation of `Encryptor` and `Decryptor` objects,
by loading a `KeyStore` file holding a symmetric key, and its use to encrypt and
decrypt data:

```scala mdoc:compile-only
import com.kevel.apso.encryption._

val encryptor = Encryptor("AES", getClass.getResourceAsStream("/keystoreFile.jceks"), "keystorePass", "keyAlias", "keyPass")

val decryptor = Decryptor("AES", getClass.getResourceAsStream("/keystoreFile.jceks"), "keystorePass", "keyAlias", "keyPass")

val secretData = "secret_info"

// encrypt data and encode it in base64; then decrypt it to string
decryptor.get.decryptToString(encryptor.get.encryptToSafeString(secretData).get)

```

## Hashing

Apso provides utilities for various hashing functions. To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-hashing" % "@VERSION@"
```

```scala mdoc:reset
import com.kevel.apso.hashing.Implicits._

"abcd".md5

"abcd".murmurHash
```

## IO

Apso provides methods to deal with IO-related features in the `io` module.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-io" % "@VERSION@"
```

### FileDescriptor

Apso introduces the concept of a `FileDescriptor`: a representation of a file stored in an arbitrary location. A descriptor includes logic to copy files to and from a local filesystem, as well as filesystem navigation logic. The following implementations of `FileDescriptor` are available:

* LocalFileDescriptor (for files in the local filesystem);
* S3FileDescriptor (for files in S3);
* SftpFileDescriptor (for files served over SFTP).

### ResourceUtil

The `ResourceUtil` object provides methods to access files available through Java's runtime environment classpath:

```scala mdoc:compile-only
import com.kevel.apso.io.ResourceUtil
// import com.kevel.apso.io.ResourceUtil

ResourceUtil.getResourceURL("reference.conf")
// res0: String = /Users/jcazevedo/work/apso/apso/target/scala-2.11/classes/reference.conf

ResourceUtil.getResourceStream("reference.conf")
// res1: java.io.InputStream = java.io.BufferedInputStream@6f16d172

ResourceUtil.getResourceAsString("reference.conf")
// res2: String =
// "apso {
//   io {
//     file-descriptor {
//       sftp.max-connections-per-host = 8
//       sftp.max-idle-time = 10s
//     }
//   }
// }
// "
```

## JSON

Apso includes a bunch of utilities to work with JSON serialization and deserialization, specifically with the [circe](https://circe.github.io/circe/) library. To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-circe" % "@VERSION@"
```

### ExtraJsonProtocol

The `ExtraJsonProtocol` object combines three traits that provide extra `Encoders` and `Decoders` (of [circe](https://circe.github.io/circe/)) for some relevant types. The `Encoders` and `Decoders` are provided on each trait for the following types:

* ExtraTimeJsonProtocol: `FiniteDuration`, `Interval` and `Period`;
* ExtraHttpJsonProtocol: `URI`;
* ExtraMiscJsonProtocol: `Config`, `DateTime`, `LocalDate` and `Currency`. It also includes the non-implicit methods `mapJsonArrayEncoder[K, V]` and `mapJsonArrayDecoder[K, V]` which serialize and deserialize a map as an array of key-value objects.

### JSON
The `json` package provides some implicits around [circe](https://circe.github.io/circe/)'s `Json` to unwrap JSON values, merge two `Json` and create `Json` from a sequence of dot-separated paths with the corresponding leaf values. It also provides methods to access and delete fields on the `Json` object. See the following for sample usage:

```scala mdoc:reset:silent
import com.kevel.apso.circe.Implicits._
import io.circe.syntax._
import io.circe.Json

"a".asJson
"2".asJson
val js1 = Json.obj(
  "a" := 2,
  "b" := 3,
  "d" := Json.obj("f" := 6))

val js2 = Json.obj(
            "c" := 4,
            "d" := Json.obj("e" := 5))
```
```scala mdoc
js1.deepMerge(js2).spaces2

fromFullPaths(Seq(
   "a" -> 1.asJson,
   "b.c" -> 2.asJson,
   "b.d" -> 3.asJson,
   "e" -> "xpto".asJson,
   "f.g.h" -> 5.asJson)).spaces2

js1.getField[Int]("a")
js1.getField[Int]("d.f")
js1.getField[Int]("x")

js1.deleteField("a")
js1.deleteField("d.f")
js1.deleteField("x")
```

### JsonConvert
The `JsonConvert` object contains helpers for converting between JSON values and other structures. See the following for sample usage:

```scala mdoc:reset
import com.kevel.apso.circe._

JsonConvert.toJson("abcd")

JsonConvert.toJson(1)

JsonConvert.toJson(Map(1 -> 2, 3 -> 4))
```

## Profiling

The `profiling` module of apso provides utilities to help with profiling the running process.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-profiling" % "@VERSION@"
```

### CpuSampler

The `CpuSampler` is a lightweight configurable CPU profiler based on call stack sampling. When run as a thread, it periodically captures the call stacks of all live threads and maintains counters for each leaf method. The counters are then dumped to a logger with a given periodicity (most probably greater than the sampling period). Each data row written to the logger contains a timestamp, the method profiled, its location in the source code and the associated absolute counters and relative weight.

### SimpleJmx

The `SimpleJmx` trait allows mixing in a simple JMX server. The JMX server is configured through a `Config` object, where the parameters `host` and `port` can be set. When behind a firewall, both the `port` defined (the RMI registry port) and the `port + 1` port (the RMI server port) need to be open. In the event of a binding failure to the defined port, a retry is performed with a random port.

## Time

The `apso-time` module provides utilities to work with `DateTime` and `LocalDate`. It mainly adds support for better working with intervals.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso-time" % "@VERSION@"
```

See the following sample usages:

```scala mdoc:reset
import com.github.nscala_time.time.Imports._

import com.kevel.apso.time._

import com.kevel.apso.time.Implicits._

(new DateTime("2012-01-01") to new DateTime("2012-01-01")).toList

(new DateTime("2012-02-01") until new DateTime("2012-03-01") by 1.day)

(new DateTime("2012-01-01") until new DateTime("2012-02-01") by 2.minutes)
```

## TestKit

Apso comes with a TestKit with extra useful matchers for [specs2](https://etorreborre.github.io/specs2/). The following traits with extra matchers are available:

* `CustomMatchers`: provides a matcher to check if an object is serializable and one to check if a file exists;
* `FutureExtraMatchers`: provides extra matchers for futures and implicit conversions for awaitables;
* `JreVersionTestHelper`: provides a wrapper for `AsResult` to only run a spec if a specific JRE version is satisfied;
