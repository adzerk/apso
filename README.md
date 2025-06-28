<p align="center"><img src="https://raw.githubusercontent.com/adzerk/apso/master/apso.png"/></p>

# Apso [![Build Status](https://github.com/adzerk/apso/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/adzerk/apso/actions/workflows/ci.yml?query=workflow%3ACI+branch%3Amaster) [![Maven Central](https://img.shields.io/maven-central/v/com.kevel/apso_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.kevel/apso_2.13)

Apso is Kevel's collection of Scala utility libraries. It provides a series of useful methods.

## Installation

Apso's latest release is built against Scala 2.13 and Scala 3.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso" % "0.23.0"
```

The project is divided in modules, you can instead install only a specific module.

The TestKit is available under the `apso-testkit` project. You can include it only for the `test` configuration:

```scala
libraryDependencies += "com.kevel" %% "apso-testkit" % "0.23.0" % "test"
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
- [Pekko HTTP](#pekko-http)
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
libraryDependencies += "com.kevel" %% "apso-core" % "0.23.0"
```

### Config

Apso provides methods to ease working with Typesafe's [config](https://github.com/typesafehub/config).

#### LazyConfigFactory

The `LazyConfigFactory` object provides static methods for creating `Config` instances in a lazy way. The lazy way refers to the variable loading process. The usual process loads variables in config files eagerly (i.e. the path needs to be defined in the same file it is refered to). The loading process provided by `LazyConfigFactory` loads and merges all configuration files and only then resolves variables. This loading process introduces a third file (beyond the default ones - `application.conf` and `reference.conf`): `overrides.conf`. This file has priority over the `application.conf` file and can be used to specify keys that should always be overriden, e.g. by environment variables.

### HTTP

Apso provides a tiny wrapper for [Dispatch](http://dispatch.databinder.net/) with synchronous operations. It's called `W`, and the following shows some sample usage:

```scala
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

```scala
import com.kevel.apso.Geo

Geo.distance((41.1617609, -8.6024716), (41.1763745, -8.5964861))
// res2: Double = 1.7004440788845807
```

You can also have the distance function curried if you are computing distances from a fixed point:

```scala
val distFromOffice = Geo.distanceFrom((41.1617609, -8.6024716))
```
```scala
distFromOffice((41.1763745, -8.5964861))
// res3: Double = 1.7004440788845807

distFromOffice((38.7223032, -9.1414664))
// res4: Double = 275.118392477037
```

### Implicits

Apso provides implicit conversions from `String`, `Seq[_]`, `Map[_, _]`, `Seq[Map[_, _]]` and `AutoCloseable` to extended types that come packed with extended features.

```scala
import com.kevel.apso.Implicits._

Seq(1, 3, 5).mergeSorted(Seq(2, 4))
// res6: Array[Int] = Array(1, 2, 3, 4, 5)

(0 to 15).average
// res7: Int = 7

Map(1 -> 2, 3 -> 6).twoWayMerge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
// res8: Map[Int, Int] = Map(2 -> 4, 3 -> 5, 1 -> 2)

Map(1 -> 2, 2 -> 4, 3 -> 6).twoWayMerge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
// res9: Map[Int, Int] = Map(2 -> 2, 3 -> 5, 1 -> 2)

Map(1 -> 2, 2 -> 3).mapKeys(_ + 1)
// res10: Map[Int, Int] = Map(2 -> 2, 3 -> 3)
```
```scala
val rand = new scala.util.Random(1)
```
```scala
rand.choose((0 to 15).toSeq)
// res11: Option[Int] = Some(value = 11)

rand.choose((0 to 15).toSeq)
// res12: Option[Int] = Some(value = 1)

rand.choose((0 to 15).toSeq)
// res13: Option[Int] = Some(value = 6)

rand.choose((0 to 15).toSeq)
// res14: Option[Int] = Some(value = 6)

rand.chooseN((0 to 15).toSeq, 4)
// res15: Seq[Int] = List(9, 8, 3, 0)

rand.chooseN((0 to 15).toSeq, 4)
// res16: Seq[Int] = List(7, 6, 5, 2)
```

### JreVersionHelper

The JreVersionHelper object provides helper methods to check the two most significant parts of the JRE version at runtime:

```scala
import com.kevel.apso.JreVersionHelper

JreVersionHelper.jreVersion
// res0: (Int, Int) = (1, 8)
```

### ProgressBar

The `ProgressBar` represents a widget to print a dynamic progress bar in a console.

```scala
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

```scala
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.kevel.apso.Retry

import java.util.concurrent.atomic.AtomicInteger

val a = new AtomicInteger()
// a: AtomicInteger = 7

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
// res20: Int = 6

var attempts = 0
// attempts: Int = 0

def m() = {
  attempts += 1
  if (attempts > 5)
    attempts
  else
    throw new Exception()
}

Retry.retry(10)(m())
// res21: util.Try[Int] = Success(value = 6)
```

## Pekko HTTP

The `pekko-http` module provides additional [directives](https://pekko.apache.org/docs/pekko-http/current/routing-dsl/directives/) to be used in [pekko-http](https://pekko.apache.org/docs/pekko-http/current/).

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-pekko-http" % "0.23.0"
```

### ClientIPDirectives

The `ClientIPDirectives` trait exposes an `optionalRawClientIP` directive that extracts the raw IP of the client from either the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP` header, in that order of priority.

### ExtraMiscDirectives

The `ExtraMiscDirectives` trait exposes the directives `cacheControlMaxAge(maxAgeDuration)` and `optionalRefererHost` to set the cache-control header to the supplied finite duration (the minimum resolution is 1 second) to extract the referer from the HTTP request header, respectively. The `ExtraMiscDirectives` companion object exposes a `cacheControlNoCache` directive to reply with the `no-cache` option in the `Cache-Control` header.

### ProxySupport

The `ProxySupport` traits adds helper methods to proxy requests to a given uri, either directly (`proxyTo`), or with the unmatched path and query parameters of the current context (`proxyToUnmatchedPath`). In order for the client IP to be correctly propagated in `X-Forward-For` headers, the `ProxySupport` trait requires the `pekko.http.server.remote-address-attribute` setting to be `on`.

## Amazon Web Services

Apso provides a group of classes to ease the interaction with the Amazon Web Services, namely S3 and EC2.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-aws" % "0.23.0"
```

### ConfigCredentialsProvider

The `ConfigCredentialsProvider` is an `AWSCredentialsProvider` (from AWS SDK for Java) that retrieves credentials from a typesafe configuration, allowing customization of its `Config` object, as well as the access key and secret key paths:

```scala
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

The `apso-caching` module provides utilities for caching, using `Caffeine` as the underlying implementation.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-caching" % "0.23.0"
```

The simplest use case is bootstrapping a cache implementation based on a configuration object:

```scala
import scala.concurrent.duration._

import com.kevel.apso.caching._

val cache = config.Cache(Some(5.seconds), None).implementation[String, Int]
// cache: com.github.blemale.scaffeine.Cache[String, Int]

val x1 = cache.getIfPresent("requests")
// x1: Option[Int] = None

cache.put("requests", 1)

val x2 = cache.getIfPresent("requests")
// x2: Option[Int] = Some(value = 1)
```

Apso also provides utilities to simplify the caching of method calls. These utilities are provided as `cachedSync()` and
`cachedAsync()` extension methods over all `FunctionN[]` types:

```scala
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.concurrent.atomic.AtomicInteger

import com.kevel.apso.caching._

val x = new AtomicInteger(0)
// x: AtomicInteger = 2

val cachedFn = ((i: Int) => {
  val value = x.getAndAdd(i)
  value
}).cachedSync(config.Cache(Some(5.seconds), None))
// cachedFn: SyncMemoizeFn1[Int, Int] = <function1>

cachedFn(2)
// res26: Int = 0
cachedFn(2)
// res27: Int = 0
x
// res28: AtomicInteger = 2

val y = new AtomicInteger(0)
// y: AtomicInteger = 3

val cachedFutFn = ((i: Int) => Future {
  val value = y.getAndAdd(i)
  value
}).cachedAsync(config.Cache(Some(5.seconds), None))
// cachedFutFn: AsyncMemoizeFn1[Int, Int] = <function1>

Await.result(cachedFutFn(3), Duration.Inf)
// res29: Int = 0
Await.result(cachedFutFn(3), Duration.Inf)
// res30: Int = 0
y
// res31: AtomicInteger = 3
```

## Collections

The `apso-collections` module provides some helpful collections. To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-collections" % "0.23.0"
```

### Trie

The `Trie` class is an implementation of an immutable trie. An example usage follows:

```scala
import com.kevel.apso.collection._

val t = Trie[Char, Int]()
// t: Trie[Char, Int] = Trie(value = None, nodes = Map())

val nt = t.set("one", 1).set("two", 2).set("three", 3).set("four", 4)
// nt: Trie[Char, Int] = Trie(
//   value = None,
//   nodes = Map(
//     'o' -> Trie(
//       value = None,
//       nodes = Map(
//         'n' -> Trie(
//           value = None,
//           nodes = Map('e' -> Trie(value = Some(value = 1), nodes = Map()))
//         )
//       )
//     ),
//     't' -> Trie(
//       value = None,
//       nodes = Map(
//         'w' -> Trie(
//           value = None,
//           nodes = Map('o' -> Trie(value = Some(value = 2), nodes = Map()))
//         ),
//         'h' -> Trie(
//           value = None,
//           nodes = Map(
//             'r' -> Trie(
//               value = None,
//               nodes = Map(
//                 'e' -> Trie(
//                   value = None,
//                   nodes = Map(
//                     'e' -> Trie(value = Some(value = 3), nodes = Map())
//                   )
//                 )
//               )
//             )
//           )
//         )
//       )
//     ),
//     'f' -> Trie(
//       value = None,
//       nodes = Map(
//         'o' -> Trie(
//           value = None,
//           nodes = Map(
//             'u' -> Trie(
//               value = None,
//               nodes = Map('r' -> Trie(value = Some(value = 4), nodes = Map()))
//             )
//           )
//         )
// ...

nt.get("one")
// res33: Option[Int] = Some(value = 1)

nt.get("two")
// res34: Option[Int] = Some(value = 2)

nt.get("five")
// res35: Option[Int] = None
```

### TypedMap

The `TypedMap` is a map that associates types with values. It can be used as follows:

```scala
import com.kevel.apso.collection._

val m = TypedMap("one", 2, 3L)
// m: TypedMap[Any] = Map(java.lang.String -> one, Int -> 2, Long -> 3)

m[String]
// res37: String = "one"

m[Int]
// res38: Int = 2

m[Long]
// res39: Long = 3L

m.get[String]
// res40: Option[String] = Some(value = "one")

m.get[Int]
// res41: Option[Int] = Some(value = 2)

m.get[Long]
// res42: Option[Long] = Some(value = 3L)

m.get[Char]
// res43: Option[Char] = None
```

### Iterators

Apso provides some utility iterators.

#### CircularIterator

The `CircularIterator` is an iterator that iterates over its elements in a circular way. See the following for sample usage:

```scala
import com.kevel.apso.iterator.CircularIterator

val circularIterator = CircularIterator(List(1, 2, 3).iterator)
// circularIterator: CircularIterator[Int] = non-empty iterator

circularIterator.take(10).toList
// res45: List[Int] = List(1, 2, 3, 1, 2, 3, 1, 2, 3, 1)
```

#### MergedBufferedIterator

The `MergedBufferedIterator` is a collection of sorted `BufferedIterators` that allows traversing them in order, while also providing a `mergeSorted` method to merge with another sorted `BufferedIterator`. See the following for sample usage:

```scala
import com.kevel.apso.iterator.MergedBufferedIterator

val it1 = MergedBufferedIterator(List(
         (0 to 3).iterator.buffered,
         (0 to 8).iterator.buffered,
         (0 to 15).iterator.buffered,
         (0 to 11).iterator.buffered))
// it1: MergedBufferedIterator[Int] = empty iterator

it1.toList
// res47: List[Int] = List(
//   0,
//   0,
//   0,
//   0,
//   1,
//   1,
//   1,
//   1,
//   2,
//   2,
//   2,
//   2,
//   3,
//   3,
//   3,
//   3,
//   4,
//   4,
//   4,
//   5,
//   5,
//   5,
//   6,
//   6,
//   6,
//   7,
//   7,
//   7,
//   8,
//   8,
//   8,
//   9,
//   9,
//   10,
//   10,
//   11,
//   11,
//   12,
//   13,
//   14,
//   15
// )

val it2 = MergedBufferedIterator(List(
         Iterator(1, 3, 5).buffered,
         Iterator(2).buffered))
// it2: MergedBufferedIterator[Int] = non-empty iterator

it2.mergeSorted(Iterator(4, 6).buffered).toList
// res48: List[Int] = List(1, 2, 3, 4, 5, 6)
```

## Encryption

Apso provides some simple utility classes to deal with encryption and decryption of data, and methods that ease the
creation of the underlying Cyphers.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-encryption" % "0.23.0"
```

The following shows the creation of `Encryptor` and `Decryptor` objects,
by loading a `KeyStore` file holding a symmetric key, and its use to encrypt and
decrypt data:

```scala
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
libraryDependencies += "com.kevel" %% "apso-hashing" % "0.23.0"
```

```scala
import com.kevel.apso.hashing.Implicits._

"abcd".md5
// res51: String = "e2fc714c4727ee9395f324cd2e7f331f"

"abcd".murmurHash
// res52: Long = 7785666560123423118L
```

## IO

Apso provides methods to deal with IO-related features in the `io` module.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-io" % "0.23.0"
```

### FileDescriptor

Apso introduces the concept of a `FileDescriptor`: a representation of a file stored in an arbitrary location. A descriptor includes logic to copy files to and from a local filesystem, as well as filesystem navigation logic. The following implementations of `FileDescriptor` are available:

* LocalFileDescriptor (for files in the local filesystem);
* S3FileDescriptor (for files in S3);
* SftpFileDescriptor (for files served over SFTP).

### ResourceUtil

The `ResourceUtil` object provides methods to access files available through Java's runtime environment classpath:

```scala
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
libraryDependencies += "com.kevel" %% "apso-circe" % "0.23.0"
```

### ExtraJsonProtocol

The `ExtraJsonProtocol` object combines three traits that provide extra `Encoders` and `Decoders` (of [circe](https://circe.github.io/circe/)) for some relevant types. The `Encoders` and `Decoders` are provided on each trait for the following types:

* ExtraTimeJsonProtocol: `FiniteDuration`, `Interval` and `Period`;
* ExtraHttpJsonProtocol: `URI`;
* ExtraMiscJsonProtocol: `Config`, `DateTime`, `LocalDate` and `Currency`. It also includes the non-implicit methods `mapJsonArrayEncoder[K, V]` and `mapJsonArrayDecoder[K, V]` which serialize and deserialize a map as an array of key-value objects.

### JSON
The `json` package provides some implicits around [circe](https://circe.github.io/circe/)'s `Json` to unwrap JSON values, merge two `Json` and create `Json` from a sequence of dot-separated paths with the corresponding leaf values. It also provides methods to access and delete fields on the `Json` object. See the following for sample usage:

```scala
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
```scala
js1.deepMerge(js2).spaces2
// res57: String = """{
//   "c" : 4,
//   "d" : {
//     "e" : 5,
//     "f" : 6
//   },
//   "a" : 2,
//   "b" : 3
// }"""

fromFullPaths(Seq(
   "a" -> 1.asJson,
   "b.c" -> 2.asJson,
   "b.d" -> 3.asJson,
   "e" -> "xpto".asJson,
   "f.g.h" -> 5.asJson)).spaces2
// res58: String = """{
//   "f" : {
//     "g" : {
//       "h" : 5
//     }
//   },
//   "e" : "xpto",
//   "b" : {
//     "d" : 3,
//     "c" : 2
//   },
//   "a" : 1
// }"""

js1.getField[Int]("a")
// res59: Option[Int] = Some(value = 2)
js1.getField[Int]("d.f")
// res60: Option[Int] = Some(value = 6)
js1.getField[Int]("x")
// res61: Option[Int] = None

js1.deleteField("a")
// res62: Json = JObject(
//   value = object[b -> 3,d -> {
//   "f" : 6
// }]
// )
js1.deleteField("d.f")
// res63: Json = JObject(
//   value = object[a -> 2,b -> 3,d -> {
//   
// }]
// )
js1.deleteField("x")
// res64: Json = JObject(
//   value = object[a -> 2,b -> 3,d -> {
//   "f" : 6
// }]
// )
```

### JsonConvert
The `JsonConvert` object contains helpers for converting between JSON values and other structures. See the following for sample usage:

```scala
import com.kevel.apso.circe._

JsonConvert.toJson("abcd")
// res66: io.circe.Json = JString(value = "abcd")

JsonConvert.toJson(1)
// res67: io.circe.Json = JNumber(value = JsonLong(value = 1L))

JsonConvert.toJson(Map(1 -> 2, 3 -> 4))
// res68: io.circe.Json = JObject(value = object[1 -> 2,3 -> 4])
```

## Profiling

The `profiling` module of apso provides utilities to help with profiling the running process.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-profiling" % "0.23.0"
```

### CpuSampler

The `CpuSampler` is a lightweight configurable CPU profiler based on call stack sampling. When run as a thread, it periodically captures the call stacks of all live threads and maintains counters for each leaf method. The counters are then dumped to a logger with a given periodicity (most probably greater than the sampling period). Each data row written to the logger contains a timestamp, the method profiled, its location in the source code and the associated absolute counters and relative weight.

### SimpleJmx

The `SimpleJmx` trait allows mixing in a simple JMX server. The JMX server is configured through a `Config` object, where the parameters `host` and `port` can be set. When behind a firewall, both the `port` defined (the RMI registry port) and the `port + 1` port (the RMI server port) need to be open. In the event of a binding failure to the defined port, a retry is performed with a random port.

## Time

The `apso-time` module provides utilities to work with `DateTime` and `LocalDate`. It mainly adds support for better working with intervals.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-time" % "0.23.0"
```

See the following sample usages:

```scala
import org.joda.time.{DateTime, Period}

import com.kevel.apso.time._

import com.kevel.apso.time.Implicits._

(new DateTime("2012-01-01") to new DateTime("2012-01-01")).toList
// res70: List[DateTime] = List(2012-01-01T00:00:00.000Z)

(new DateTime("2012-02-01") until new DateTime("2012-03-01") by Period.days(1))
// res71: IterableInterval = IndexedSeq(
//   2012-02-01T00:00:00.000Z,
//   2012-02-02T00:00:00.000Z,
//   2012-02-03T00:00:00.000Z,
//   2012-02-04T00:00:00.000Z,
//   2012-02-05T00:00:00.000Z,
//   2012-02-06T00:00:00.000Z,
//   2012-02-07T00:00:00.000Z,
//   2012-02-08T00:00:00.000Z,
//   2012-02-09T00:00:00.000Z,
//   2012-02-10T00:00:00.000Z,
//   2012-02-11T00:00:00.000Z,
//   2012-02-12T00:00:00.000Z,
//   2012-02-13T00:00:00.000Z,
//   2012-02-14T00:00:00.000Z,
//   2012-02-15T00:00:00.000Z,
//   2012-02-16T00:00:00.000Z,
//   2012-02-17T00:00:00.000Z,
//   2012-02-18T00:00:00.000Z,
//   2012-02-19T00:00:00.000Z,
//   2012-02-20T00:00:00.000Z,
//   2012-02-21T00:00:00.000Z,
//   2012-02-22T00:00:00.000Z,
//   2012-02-23T00:00:00.000Z,
//   2012-02-24T00:00:00.000Z,
//   2012-02-25T00:00:00.000Z,
//   2012-02-26T00:00:00.000Z,
//   2012-02-27T00:00:00.000Z,
//   2012-02-28T00:00:00.000Z,
//   2012-02-29T00:00:00.000Z
// )

(new DateTime("2012-01-01") until new DateTime("2012-02-01") by Period.minutes(2))
// res72: IterableInterval = IndexedSeq(
//   2012-01-01T00:00:00.000Z,
//   2012-01-01T00:02:00.000Z,
//   2012-01-01T00:04:00.000Z,
//   2012-01-01T00:06:00.000Z,
//   2012-01-01T00:08:00.000Z,
//   2012-01-01T00:10:00.000Z,
//   2012-01-01T00:12:00.000Z,
//   2012-01-01T00:14:00.000Z,
//   2012-01-01T00:16:00.000Z,
//   2012-01-01T00:18:00.000Z,
//   2012-01-01T00:20:00.000Z,
//   2012-01-01T00:22:00.000Z,
//   2012-01-01T00:24:00.000Z,
//   2012-01-01T00:26:00.000Z,
//   2012-01-01T00:28:00.000Z,
//   2012-01-01T00:30:00.000Z,
//   2012-01-01T00:32:00.000Z,
//   2012-01-01T00:34:00.000Z,
//   2012-01-01T00:36:00.000Z,
//   2012-01-01T00:38:00.000Z,
//   2012-01-01T00:40:00.000Z,
//   2012-01-01T00:42:00.000Z,
//   2012-01-01T00:44:00.000Z,
//   2012-01-01T00:46:00.000Z,
//   2012-01-01T00:48:00.000Z,
//   2012-01-01T00:50:00.000Z,
//   2012-01-01T00:52:00.000Z,
//   2012-01-01T00:54:00.000Z,
//   2012-01-01T00:56:00.000Z,
//   2012-01-01T00:58:00.000Z,
//   2012-01-01T01:00:00.000Z,
//   2012-01-01T01:02:00.000Z,
//   2012-01-01T01:04:00.000Z,
//   2012-01-01T01:06:00.000Z,
//   2012-01-01T01:08:00.000Z,
//   2012-01-01T01:10:00.000Z,
//   2012-01-01T01:12:00.000Z,
//   2012-01-01T01:14:00.000Z,
//   2012-01-01T01:16:00.000Z,
//   2012-01-01T01:18:00.000Z,
//   2012-01-01T01:20:00.000Z,
//   2012-01-01T01:22:00.000Z,
//   2012-01-01T01:24:00.000Z,
//   2012-01-01T01:26:00.000Z,
//   2012-01-01T01:28:00.000Z,
//   2012-01-01T01:30:00.000Z,
//   2012-01-01T01:32:00.000Z,
//   2012-01-01T01:34:00.000Z,
// ...
```

## TestKit

Apso comes with a TestKit with extra useful matchers for [specs2](https://etorreborre.github.io/specs2/). The following traits with extra matchers are available:

* `CustomMatchers`: provides a matcher to check if an object is serializable and one to check if a file exists;
* `FutureExtraMatchers`: provides extra matchers for futures and implicit conversions for awaitables;
* `JreVersionTestHelper`: provides a wrapper for `AsResult` to only run a spec if a specific JRE version is satisfied.

To use the version for version 4 of `specs2`, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-testkit" % "0.23.0"
```

To use the version for version 5 of `specs2` (only available for Scala 3), add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.kevel" %% "apso-testkit-specs2-5" % "0.23.0"
```
