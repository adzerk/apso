<p align="center"><img src="https://raw.githubusercontent.com/velocidi/apso/master/apso.png"/></p>

# Apso [![Build Status](https://travis-ci.org/velocidi/apso.svg?branch=master)](https://travis-ci.org/velocidi/apso) [![Maven Central](https://img.shields.io/maven-central/v/com.velocidi/apso_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.velocidi/apso_2.12)

Apso is Velocidi's Scala utilities library. It provides a series of useful methods.

## Installation

Apso's latest release is built against Scala 2.12.

To use it in an existing SBT project, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.velocidi" %% "apso" % "0.14.0"
```

The TestKit is available under the `apso-testkit` project. You can include it only for the `test` configuration:

```scala
libraryDependencies += "com.velocidi" %% "apso-testkit" % "0.14.0" % "test"
```

Please take into account that the library is still in an experimental stage and the interfaces might change for subsequent releases.

## Table of Contents

- [Geo](#geo)
- [JreVersionHelper](#jreversionhelper)
- [Logging](#logging)
- [ProgressBar](#progressbar)
- [Reflect](#reflect)
- [Retry](#retry)
- [TryWith](#trywith)
- [Implicits](#implicits)
- [Amazon Web Services](#amazon-web-services)
    - [ConfigCredentialsProvider](#configcredentialsprovider)
    - [CredentialStore](#credentialstore)
    - [S3Bucket](#s3bucket)
    - [SerializableAWSCredentials](#serializableawscredentials)
- [Collections](#collections)
    - [Trie](#trie)
    - [TypedMap](#typedmap)
- [Config](#config)
    - [LazyConfigFactory](#lazyconfigfactory)
- [Encryption](#encryption)
- [Hashing](#hashing)
- [HTTP](#http)
- [IO](#io)
    - [FileDescriptor](#filedescriptor)
    - [ResourceUtil](#resourceutil)
- [Iterators](#iterators)
    - [CircularIterator](#circulariterator)
    - [CompositeIterator](#compositeiterator)
    - [MergedBufferedIterator](#mergedbufferediterator)
- [JSON](#json)
    - [ExtraJsonProtocol](#extrajsonprotocol)
    - [JsValue](#jsvalue)
    - [JsonConvert](#jsonconvert)
    - [JsonFormatBuilder](#jsonformatbuilder)
- [Profiling](#profiling)
    - [CpuSampler](#cpusampler)
    - [SimpleJmx](#simplejmx)
- [Spray](#spray)
    - [ClientIPDirectives](#clientipdirectives)
    - [ExtraMiscDirectives](#extramiscdirectives)
    - [Implicits](#implicits)
    - [ProxySupport](#proxysupport)
- [Time](#time)
- [TestKit](#testkit)

## Geo

The `Geo` object provides methods to compute distances in kilometers between two points on the planet Earth, calculated using the spherical [law of cosines](https://en.wikipedia.org/wiki/Great-circle_distance#Formulas). Coordinates are represented by a pair of `Double` for latitude and longitude.

```scala
scala> import com.velocidi.apso.Geo
import com.velocidi.apso.Geo

scala> Geo.distance((41.1617609, -8.6024716), (41.1763745, -8.5964861))
res0: Double = 1.7004440762344684
```

You can also have the distance function curried if you are computing distances from a fixed point:

```scala
scala> val distFromOffice = Geo.distanceFrom((41.1617609, -8.6024716))
distToOffice: com.velocidi.apso.Geo.Coordinates => Double = <function1>

scala> distFromOffice((41.1763745, -8.5964861))
res1: Double = 1.7004440762344684

scala> distFromOffice((38.7223032, -9.1414664))
res2: Double = 275.118392477037
```

## JreVersionHelper

The JreVersionHelper object provides helper methods to check the two most significant parts of the JRE version at runtime:

```scala
scala> import com.velocidi.apso.JreVersionHelper
import com.velocidi.apso.JreVersionHelper

scala> JreVersionHelper.jreVersion
res0: (Int, Int) = (1,8)
```

## Logging

The `Logging` and `StrictLogging` traits allows mixing in Log4j2 `Logger` objects. The difference between the two is that in the former the `Logger` object is initialized lazily, while in the latter it is initialized strictly:

```scala
scala> class A extends Logging {}
defined class A

scala> val a = new A
a: A = A@58af6f21

scala> a.log.info("test")
...
```

## ProgressBar

The `ProgressBar` represents a widget to print a dynamic progress bar in a console.

```scala
scala> import com.velocidi.apso.ProgressBar
import com.velocidi.apso.ProgressBar

scala> val progress = ProgressBar(100)
progress: com.velocidi.apso.ProgressBar = ProgressBar(100)

scala> progress.tick(1)
  1% [>                                                     ] / [ 0.19 ] ops/s

scala> progress.tick(2)
  3% [=>                                                    ] - [ 0.15 ] ops/s

scala> progress.tick(1)
  4% [==>                                                   ] \ [ 0.12 ] ops/s

scala> progress.tick(10)
 14% [=======>                                              ] | [ 0.31 ] ops/s

scala> progress.tick(20)
 34% [==================>                                   ] / [ 0.46 ] ops/s

scala> progress.tick(30)
 64% [=================================>                    ] - [ 0.77 ] ops/s
```

## Reflect

The `Reflect` object contains helpers for reflection-related tasks, namely to create an instance of a given class given its fully qualified name and also to access singleton objects:

```scala
scala> import com.velocidi.apso.Reflect
import com.velocidi.apso.Reflect

scala> import com.velocidi.apso.collection._
import com.velocidi.apso.collection._

scala> Reflect.newInstance[HMap[Nothing]]("com.velocidi.apso.collection.HMap")
res0: com.velocidi.apso.collection.HMap[Nothing] = HMap()

scala> Reflect.companion[Reflect.type]("com.velocidi.apso.Reflect")
res1: com.velocidi.apso.Reflect.type = com.velocidi.apso.Reflect$@3b1dbca
```

## Retry

The `Retry` object provides a method to retry methods or `Future`s a given number of times until they succeed or the specified maximum number of retries is reached:

```scala
scala> import scala.concurrent.Future
import scala.concurrent.Future
scala> import com.velocidi.apso.Retry
import com.velocidi.apso.Retry

scala> import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global

scala> import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicInteger

scala> val a = new AtomicInteger()
a: java.util.concurrent.atomic.AtomicInteger = 0

scala> def f: Future[Int] = {
     |   Future {
     |     val value = a.getAndAdd(1)
     |     if (value > 5)
     |       value
     |     else {
     |       throw new Exception()
     |     }
     |   }
     | }
f: scala.concurrent.Future[Int]

scala> Retry.retryFuture(10)(f).onComplete(println)
Success(6)

scala> var attempts = 0
var attempts = 0

scala> def m() = {
     |   attempts += 1
     |   if (attempts > 5)
     |     attempts
     |   else
     |     throw new Exception()
     | }
     
scala> println(Retry.retry(10)(m))
Success(6)
```

## TryWith

The `TryWith` object mimics the [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) 
construct from Java world, or a loan pattern, where a given function can try to use a `Closeable` 
resource which shall automatically be disposed off and closed properly afterwards.

```scala
scala> import java.io.Closeable
import com.velocidi.apso.TryWith

scala> import com.velocidi.apso.TryWith
import com.velocidi.apso.TryWith

scala> def buildResource = new Closeable {
     |   override def toString: String = "good resource"
     |   def close(): Unit = {
     |     println("Resource is now Closed")
     |   }
     | }

scala> def goodHandler(resource: Closeable) = {
     |   println(resource)
     | }

scala> def badHandler(resource: Closeable) = {
     |   throw new Exception()
     | }

scala> TryWith(buildResource)(goodHandler)
good resource
Resource is now Closed
res2: scala.util.Try[Unit] = Success(())

scala> TryWith(buildResource)(badHandler)
Resource is now Closed
res3: scala.util.Try[Nothing] = Failure(java.lang.Exception)
```

## Implicits

Apso provides implicit conversions from `String`, `Seq[_]`, `Map[_, _]`, `Seq[Map[_, _]]` and `AutoCloseable` to extended types that come packed with extended features.

```scala
scala> import com.velocidi.apso.Implicits._
import com.velocidi.apso.Implicits._

scala> Seq(1, 3, 5).mergeSorted(Seq(2, 4))
res9: Seq[Int] = List(1, 2, 3, 4, 5)

scala> (0 to 15).average
res10: Int = 7

scala> Iterator(1, 3, 5).buffered.mergeSorted(Iterator(2, 4).buffered).toList
res11: List[Int] = List(1, 2, 3, 4, 5)

scala> Map(1 -> 2, 3 -> 6).twoWayMerge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
res16: Map[Int,Int] = Map(2 -> 4, 3 -> 5, 1 -> 2)

scala> Map(1 -> 2, 2 -> 4, 3 -> 6).twoWayMerge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
res17: Map[Int,Int] = Map(2 -> 2, 3 -> 5, 1 -> 2)

scala> Map(1 -> 2, 2 -> 3).mapKeys(_ + 1)
res18: Map[Int,Int] = Map(2 -> 2, 3 -> 3)

scala> scala.util.Random.choose((0 to 15).toSeq)
res22: Option[Int] = Some(15)

scala> scala.util.Random.choose((0 to 15).toSeq)
res23: Option[Int] = Some(12)

scala> scala.util.Random.choose((0 to 15).toSeq)
res24: Option[Int] = Some(9)

scala> scala.util.Random.choose((0 to 15).toSeq)
res25: Option[Int] = Some(2)

scala> scala.util.Random.chooseN((0 to 15).toSeq, 4)
res26: Seq[Int] = List(9, 8, 7, 6)

scala> scala.util.Random.chooseN((0 to 15).toSeq, 4)
res27: Seq[Int] = List(8, 5, 2, 1)
```

## Amazon Web Services

Apso provides a group of classes to ease the interaction with the Amazon Web Services, namely S3 and EC2.

### ConfigCredentialsProvider

The `ConfigCredentialsProvider` is an `AWSCredentialsProvider` (from AWS SDK for Java) that retrieves credentials from a typesafe configuration, allowing customization of its `Config` object, as well as the access key and secret key paths:

```scala
scala> import com.velocidi.apso.aws._
import com.velocidi.apso.aws._

scala> import com.typesafe.config._
import com.typesafe.config._

scala> val confProvider = ConfigCredentialsProvider(
     |   config = ConfigFactory.parseString("""{
     |     aws {
     |       access-key = "<access-key>"
     |       secret-key = "<secret-key>"
     |     }
     |   }"""),
     |   accessKeyPath = "aws.access-key",
     |   secretKeyPath = "aws.secret-key")
confProvider: com.velocidi.apso.aws.ConfigCredentialsProvider = ConfigCredentialsProvider(Config(SimpleConfigObject({"aws":{"access-key":"<access-key>","secret-key":"<secret-key>"}})),aws.access-key,aws.secret-key)

scala> val credentials = confProvider.getCredentials
credentials: com.amazonaws.auth.AWSCredentials = com.amazonaws.auth.BasicAWSCredentials@46056cef

scala> credentials.getAWSAccessKeyId
res0: String = <access-key>

scala> credentials.getAWSSecretKey
res1: String = <secret-key>
```

### CredentialStore

The `CredentialStore` object serves as an endpoint for the retrieval of AWS credentials from available configurations. It extends the chain in the `DefaultAWSCredentialsProviderChain` (from AWS SDK for Java) with the retrieval of AWS credentials through the default typesafe configuration file (typically `application.conf`).

### S3Bucket

The `S3Bucket` class wraps an instance of `AmazonS3Client` (from AWS SDK for Java) and exposes a higher level interface for pushing and pulling files to and from a bucket.

### SerializableAWSCredentials

The `SerializableAWSCredentials` class provides a serializable container for AWS credentials, extending the `AWSCredentials` class (from AWS SDK for Java).

## Collections

The `com.velocidi.apso.collection` package provides some helpful collections:

### Trie

The `Trie` class is an implementation of an immutable trie. An example usage follows:

```scala
scala> import com.velocidi.apso.collection._
import com.velocidi.apso.collection._

scala> val t = Trie[Char, Int]()
t: com.velocidi.apso.collection.Trie[Char,Int] = Trie(None,Map())

scala> val nt = t.set("one", 1).set("two", 2).set("three", 3).set("four", 4)
nt: com.velocidi.apso.collection.Trie[Char,Int] = Trie(None,Map(o -> Trie(None,Map(n -> Trie(None,Map(e -> Trie(Some(1),Map()))))), t -> Trie(None,Map(w -> Trie(None,Map(o -> Trie(Some(2),Map()))), h -> Trie(None,Map(r -> Trie(None,Map(e -> Trie(None,Map(e -> Trie(Some(3),Map()))))))))), f -> Trie(None,Map(o -> Trie(None,Map(u -> Trie(None,Map(r -> Trie(Some(4),Map())))))))))

scala> nt.get("one")
res0: Option[Int] = Some(1)

scala> nt.get("two")
res1: Option[Int] = Some(2)

scala> nt.get("five")
res2: Option[Int] = None
```

### TypedMap

The `TypedMap` is a map that associates types with values. It can be used as follows:

```scala
scala> val m = TypedMap("one", 2, 3l)
m: com.velocidi.apso.collection.TypedMap[Any] = Map(java.lang.String -> one, Int -> 2, Long -> 3)

scala> m[String]
res0: String = one

scala> m[Int]
res1: Int = 2

scala> m[Long]
res2: Long = 3

scala> m.get[String]
res3: Option[String] = Some(one)

scala> m.get[Int]
res4: Option[Int] = Some(2)

scala> m.get[Long]
res5: Option[Long] = Some(3)

scala> m.get[Char]
res6: Option[Char] = None
```

## Config

Apso provides methods to ease working with Typesafe's [config](https://github.com/typesafehub/config).

### LazyConfigFactory

The `LazyConfigFactory` object provides static methods for creating `Config` instances in a lazy way. The lazy way refers to the variable loading process. The usual process loads variables in config files eagerly (i.e. the path needs to be defined in the same file it is refered to). The loading process provided by `LazyConfigFactory` loads and merges all configuration files and only then resolves variables. This loading process introduces a third file (beyond the default ones - `application.conf` and `reference.conf`): `overrides.conf`. This file has priority over the `application.conf` file and can be used to specify keys that should always be overriden, e.g. by environment variables.

## Encryption

Apso provides some simple utility classes to deal with encryption and decryption of data, and methods that ease the
creation of the underlying Cyphers.

The following shows the creation of `Encryptor` and `Decryptor` objects,
by loading a `KeyStore` file holding a symmetric key, and its use to encrypt and
decrypt data:

```scala
scala> val encryptor = Encryptor("AES", getClass.getResourceAsStream("/keystoreFile.jceks"), "keystorePass", "keyAlias", "keyPass")
encryptor: Option[com.velocidi.apso.encryption.Encryptor] = Some(com.velocidi.apso.encryption.Encryptor@353912)

scala> val decryptor = Decryptor("AES", getClass.getResourceAsStream("/keystoreFile.jceks"), "keystorePass", "keyAlias", "keyPass")
decryptor: Option[com.velocidi.apso.encryption.Decryptor] = Some(com.velocidi.apso.encryption.Decryptor@68ccfc03)

scala> val secretData = "secret_info"
secretData: String = secret_info

// encrypt data and encode it in base64; then decrypt it to string
scala> decryptor.get.decryptToString(encryptor.get.encryptToSafeString(secretData).get)
res6: Option[String] = Some(secret_info)

```


## Hashing

Apso provides utilities for various hashing functions.

```scala
scala> import com.velocidi.apso.hashing.Implicits._
import com.velocidi.apso.hashing.Implicits._

scala> "abcd".md5
res0: String = e2fc714c4727ee9395f324cd2e7f331f

scala> "abcd".murmurHash
res1: Long = 7785666560123423118
```

## HTTP

Apso provides a tiny wrapper for [Dispatch](http://dispatch.databinder.net/) with synchronous operations. It's called `W`, and the following shows some sample usage:

```scala
scala> import com.velocidi.apso.http.W
import com.velocidi.apso.http.W

scala> W.get("http://www.google.com/").getStatusCode
res0: Int = 302

scala> W.post("http://www.google.com/", "").getStatusCode
res1: Int = 405

scala> W.put("http://www.google.com/", "").getStatusCode
res2: Int = 405

scala> W.delete("http://www.google.com/").getStatusCode
res3: Int = 405

scala> W.head("http://www.google.com/").getStatusCode
res4: Int = 302
```

The POST and PUT methods can also receive the body as `JSON` (of [circe](https://github.com/circe/circe)), which adds the `Content-type` header accordingly.

## IO

Apso provides methods to deal with IO-related features in the `io` package.

### FileDescriptor

Apso introduces the concept of a `FileDescriptor`: a representation of a file stored in an arbitrary location. A descriptor includes logic to copy files to and from a local filesystem, as well as filesystem navigation logic. The following implementations of `FileDescriptor` are available:

* LocalFileDescriptor (for files in the local filesystem);
* S3FileDescriptor (for files in S3);
* SftpFileDescriptor (for files served over SFTP).

### ResourceUtil

The `ResourceUtil` object provides methods to access files available through Java's runtime environment classpath:

```scala
scala> import com.velocidi.apso.io.ResourceUtil
import com.velocidi.apso.io.ResourceUtil

scala> ResourceUtil.getResourceURL("reference.conf")
res0: String = /Users/jcazevedo/work/apso/apso/target/scala-2.11/classes/reference.conf

scala> ResourceUtil.getResourceStream("reference.conf")
res1: java.io.InputStream = java.io.BufferedInputStream@6f16d172

scala> ResourceUtil.getResourceAsString("reference.conf")
res2: String =
"apso {
  io {
    file-descriptor {
      sftp.max-connections-per-host = 8
      sftp.max-idle-time = 10s
    }
  }
}
"
```

## Iterators

Apso provides some utility iterators.

### CircularIterator

The `CircularIterator` is an iterator that iterates over its elements in a circular way. See the following for sample usage:

```scala
scala> import com.velocidi.apso.iterator.CircularIterator
import com.velocidi.apso.iterator.CircularIterator

scala> val circularIterator = CircularIterator(List(1, 2, 3).toIterator)
circularIterator: com.velocidi.apso.iterator.CircularIterator[Int] = non-empty iterator

scala> circularIterator.take(10).toList
res0: List[Int] = List(1, 2, 3, 1, 2, 3, 1, 2, 3, 1)
```

### CompositeIterator

The `CompositeIterator` is an iterator that wraps a list of other iterators and iterates over its elements sequentially. It handles compositions of a large number of iterators in a more efficient way than simply concatenating them, avoiding stack overflows in particular. It supports appending of new iterators while keeping its efficiency. See the following for sample usage:

```scala
scala> import com.velocidi.apso.iterator.CompositeIterator
import com.velocidi.apso.iterator.CompositeIterator

scala> val compositeIterator = CompositeIterator(List(1, 2, 3).toIterator, List(4, 5, 6).toIterator, List(7, 8, 9).toIterator)
compositeIterator: com.velocidi.apso.iterator.CompositeIterator[Int] = non-empty iterator

scala> compositeIterator.take(9).toList
res0: List[Int] = List(1, 2, 3, 4, 5, 6, 7, 8, 9)
```

### MergedBufferedIterator

The `MergedBufferedIterator` is a collection of sorted `BufferedIterators` that allows traversing them in order, while also providing a `mergeSorted` method to merge with another sorted `BufferedIterator`. See the following for sample usage:

```scala
scala> import com.velocidi.apso.iterator.MergedBufferedIterator
import com.velocidi.apso.iterator.MergedBufferedIterator

scala> val it1 = MergedBufferedIterator(List(
     |   (0 to 3).toIterator.buffered,
     |   (0 to 8).toIterator.buffered,
     |   (0 to 15).toIterator.buffered,
     |   (0 to 11).toIterator.buffered))
it1: com.velocidi.apso.iterator.MergedBufferedIterator[Int] = non-empty iterator

scala> it1.toList
res0: List[Int] = List(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 10, 10, 11, 11, 12, 13, 14, 15)

scala> val it2 = MergedBufferedIterator(List(
     |   Iterator(1, 3, 5).buffered,
     |   Iterator(2).buffered))
it2: com.velocidi.apso.iterator.MergedBufferedIterator[Int] = non-empty iterator

scala> it2.mergeSorted(Iterator(4, 6).buffered).toList
res1: List[Int] = List(1, 2, 3, 4, 5, 6)
```

## JSON

Apso includes a bunch of utilities to work with JSON serialization and deserialization.

### ExtraJsonProtocol

The `ExtraJsonProtocol` object combines three traits that provide extra `JsonFormats` (of [spray-json](https://github.com/spray/spray-json)) for some relevant types. The `JsonFormats` that are provided en each trait are the following:

* ExtraTimeJsonProtocol: `JsonFormat[FiniteDuration]` and `JsonFormat[Interval]`;
* ExtraHttpJsonProtocol: `JsonFormat[URI]`;
* ExtraMiscJsonProtocol: `JsonFormat[Config]`, `JsonFormat[DateTime]` and `JsonFormat[LocalDate]`. It also includes the non-implicit method `mapJsArrayFormat[K, V]` which serializes a map as an array of key-value objects. 
Note that `spray-json`'s `JsonFormat` for maps has the same signature, so if you need to use both at the same time, you need to extend the `DefaultJsonProtocol` trait instead of importing it.

### JsValue

The `json` package provides some implicits around [spray-json](https://github.com/spray/spray-json)'s `JsValue` to unwrap JSON values, merge two `JsValues` and create `JsValues` from a sequence of dot-separated paths with the corresponding leaf values. See the following for sample usage:

```scala
scala> import com.velocidi.apso.json.Implicits._
import com.velocidi.apso.json.Implicits._

scala> import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol._

scala> import spray.json._
import spray.json._

scala> "a".toJson.toValue
res0: Any = a

scala> "2".toJson.toValue
res1: Any = 2

scala> val js1 = """{
     |   "a": 2,
     |   "b": 3,
     |   "d": {
     |     "f": 6
     |   }
     | }""".parseJson.asJsObject
js1: spray.json.JsObject = {"a":2,"b":3,"d":{"f":6}}

scala> val js2 = """{
     |   "c": 4,
     |   "d": {
     |     "e": 5
     |   }
     | }""".parseJson.asJsObject
js2: spray.json.JsObject = {"c":4,"d":{"e":5}}

scala> js1.merge(js2).prettyPrint
res2: String =
{
  "c": 4,
  "d": {
    "e": 5,
    "f": 6
  },
  "a": 2,
  "b": 3
}

scala> fromFullPaths(Seq(
     |   "a" -> 1.toJson,
     |   "b.c" -> 2.toJson,
     |   "b.d" -> 3.toJson,
     |   "e" -> "xpto".toJson,
     |   "f.g.h" -> 5.toJson)).prettyPrint
res3: String =
{
  "f": {
    "g": {
      "h": 5
    }
  },
  "e": "xpto",
  "b": {
    "d": 3,
    "c": 2
  },
  "a": 1
}
```

### JsonConvert

The `JsonConvert` object contains helpers for converting between JSON values and other structures. See the following for sample usage:

```scala
scala> import com.velocidi.apso.json._
import com.velocidi.apso.json._

scala> JsonConvert.toJson("abcd")
res0: spray.json.JsValue = "abcd"

scala> JsonConvert.toJson(1)
res1: spray.json.JsValue = 1

scala> JsonConvert.toJson(Map(1 -> 2, 3 -> 4))
res2: spray.json.JsValue = {"1":2,"3":4}
```

### JsonFormatBuilder

The `JsonFormatBuilder` class provides a type-safe way to construct a `JsonFormat` by incrementally adding, removing or updating fields. See the following for sample usage:

```scala
scala> import com.velocidi.apso.json._
import com.velocidi.apso.json._

scala> import shapeless._
import shapeless._

scala> import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol._

scala> import spray.json._
import spray.json._

scala> case class Test(a: Int, b: List[String], c: Double)
defined class Test

scala> val builder = JsonFormatBuilder().field[Int]("a").field[List[String]]("b")
builder: com.velocidi.apso.json.JsonFormatBuilder[shapeless.::[Int,shapeless.::[List[String],shapeless.HNil]],shapeless.::[com.velocidi.apso.json.JsonFormatBuilder.Field[Int],shapeless.::[com.velocidi.apso.json.JsonFormatBuilder.Field[List[String]],shapeless.HNil]]] = JsonFormatBuilder(Field(a,spray.json.BasicFormats$IntJsonFormat$@50ef5db8,None) :: Field(b,spray.json.CollectionFormats$$anon$1@4bc54f85,None) :: HNil)

scala> val jf1 = builder.jsonFormat[Test](
     |   { case a :: b :: HNil => Test(a, b, -1.0) },
     |   { test => test.a :: test.b :: HNil })
jf1: spray.json.RootJsonFormat[Test] = com.velocidi.apso.json.JsonFormatBuilder$$anon$1@70ae62e7

scala> """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf1)
res0: Test = Test(3,List(x, y),-1.0)

scala> """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf1)
res1: Test = Test(3,List(x, y),-1.0)

scala> Test(3, List("x", "y"), -1.0).toJson(jf1)
res2: spray.json.JsValue = {"b":["x","y"],"a":3}

scala> val builder2 = builder.field[Double]("c", 0.0)
builder2: com.velocidi.apso.json.JsonFormatBuilder[shapeless.::[Int,shapeless.::[List[String],shapeless.::[Double,shapeless.HNil]]],shapeless.::[com.velocidi.apso.json.JsonFormatBuilder.Field[Int],shapeless.::[com.velocidi.apso.json.JsonFormatBuilder.Field[List[String]],shapeless.::[com.velocidi.apso.json.JsonFormatBuilder.Field[Double],shapeless.HNil]]]] = JsonFormatBuilder(Field(a,spray.json.BasicFormats$IntJsonFormat$@50ef5db8,None) :: Field(b,spray.json.CollectionFormats$$anon$1@4bc54f85,None) :: Field(c,spray.json.BasicFormats$DoubleJsonFormat$@73930900,Some(0.0)) :: HNil)

scala> val jf2 = builder2.jsonFormat[Test](
     |   { case a :: b :: c :: HNil => Test(a, b, c) },
     |   { test => test.a :: test.b :: test.c :: HNil })
jf2: spray.json.RootJsonFormat[Test] = com.velocidi.apso.json.JsonFormatBuilder$$anon$1@6ce596d9

scala> """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf2)
res3: Test = Test(3,List(x, y),0.0)

scala> """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf2)
res4: Test = Test(3,List(x, y),3.0)

scala> Test(3, List("x", "y"), 0.0).toJson(jf2)
res5: spray.json.JsValue = {"c":0.0,"b":["x","y"],"a":3}
```

## Profiling

The `profiling` package of apso provides utilities to help with profiling the running process.

### CpuSampler

The `CpuSampler` is a lightweight configurable CPU profiler based on call stack sampling. When run as a thread, it periodically captures the call stacks of all live threads and maintains counters for each leaf method. The counters are then dumped to a logger with a given periodicity (most probably greater than the sampling period). Each data row written to the logger contains a timestamp, the method profiled, its location in the source code and the associated absolute counters and relative weight.

### SimpleJmx

The `SimpleJmx` trait allows mixing in a simple JMX server. The JMX server is configured through a `Config` object, where the parameters `host` and `port` can be set. When behind a firewall, both the `port` defined (the RMI registry port) and the `port + 1` port (the RMI server port) need to be open. In the event of a binding failure to the defined port, a retry is performed with a random port.

## Spray

The `spray` package provides additional [directives](http://spray.io/documentation/1.2.2/spray-routing/key-concepts/directives/) to be used in [spray-routing](https://github.com/spray/spray).

### ClientIPDirectives

The `ClientIPDirectives` trait exposes an `optionalRawClientIP` directive that extracts the raw IP of the client from either the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP` header, in that order of priority.

### ExtraMiscDirectives

The `ExtraMiscDirectives` trait exposes the directives `cacheControlMaxAge(maxAgeDuration)` and `optionalRefererHost` to set the cache-control header to the supplied finite duration (the minimum resolution is 1 second) to extract the referer from the HTTP request header, respectively. The `ExtraMiscDirectives` companion object exposes a `cacheControlNoCache` directive to reply with the `no-cache` option in the `Cache-Control` header.

### Implicits

The `Implicits` companion object exposes an implicit method that provides a [`Marshaller`](http://spray.io/documentation/1.2.2/spray-httpx/marshalling/) for Scalaz's `Validation`.

### ProxySupport

The `ProxySupport` traits adds helper methods to proxy requests to a given uri, either directly (`proxyTo`), or with the unmatched path and query parameters of the current context (`proxyToUnmatchedPath`).

## Time

The `com.velocidi.apso.time` package provides utilities to work with `DateTime` and `LocalDate`. It mainly adds support for better working with intervals. See the following sample usage:

```scala
scala> import com.github.nscala_time.time.Imports._
import com.github.nscala_time.time.Imports._

scala> import com.velocidi.apso.time._
import com.velocidi.apso.time._

scala> import com.velocidi.apso.time.Implicits._
import com.velocidi.apso.time.Implicits._

scala> (new DateTime("2012-01-01") to new DateTime("2012-01-01")).toList
res0: List[com.github.nscala_time.time.Imports.DateTime] = List(2012-01-01T00:00:00.000Z)

scala> (new DateTime("2012-02-01") until new DateTime("2012-03-01") by 1.day)
res1: com.velocidi.apso.time.IterableInterval = SteppedInterval(2012-02-01T00:00:00.000Z, 2012-02-02T00:00:00.000Z, 2012-02-03T00:00:00.000Z, 2012-02-04T00:00:00.000Z, 2012-02-05T00:00:00.000Z, 2012-02-06T00:00:00.000Z, 2012-02-07T00:00:00.000Z, 2012-02-08T00:00:00.000Z, 2012-02-09T00:00:00.000Z, 2012-02-10T00:00:00.000Z, 2012-02-11T00:00:00.000Z, 2012-02-12T00:00:00.000Z, 2012-02-13T00:00:00.000Z, 2012-02-14T00:00:00.000Z, 2012-02-15T00:00:00.000Z, 2012-02-16T00:00:00.000Z, 2012-02-17T00:00:00.000Z, 2012-02-18T00:00:00.000Z, 2012-02-19T00:00:00.000Z, 2012-02-20T00:00:00.000Z, 2012-02-21T00:00:00.000Z, 2012-02-22T00:00:00.000Z, 2012-02-23T00:00:00.000Z, 2012-02-24T00:00:00.000Z, 2012-02-25T00:00:00.000Z, 2012-02-26T00:00:00.000Z, 2012-02-27T00:00:00.000Z, 2012-02-28T00:00:00.000Z, 20...

scala> (new DateTime("2012-01-01") until new DateTime("2012-02-01") by 2.minutes)
res2: com.velocidi.apso.time.IterableInterval = SteppedInterval(2012-01-01T00:00:00.000Z, 2012-01-01T00:02:00.000Z, 2012-01-01T00:04:00.000Z, 2012-01-01T00:06:00.000Z, 2012-01-01T00:08:00.000Z, 2012-01-01T00:10:00.000Z, 2012-01-01T00:12:00.000Z, 2012-01-01T00:14:00.000Z, 2012-01-01T00:16:00.000Z, 2012-01-01T00:18:00.000Z, 2012-01-01T00:20:00.000Z, 2012-01-01T00:22:00.000Z, 2012-01-01T00:24:00.000Z, 2012-01-01T00:26:00.000Z, 2012-01-01T00:28:00.000Z, 2012-01-01T00:30:00.000Z, 2012-01-01T00:32:00.000Z, 2012-01-01T00:34:00.000Z, 2012-01-01T00:36:00.000Z, 2012-01-01T00:38:00.000Z, 2012-01-01T00:40:00.000Z, 2012-01-01T00:42:00.000Z, 2012-01-01T00:44:00.000Z, 2012-01-01T00:46:00.000Z, 2012-01-01T00:48:00.000Z, 2012-01-01T00:50:00.000Z, 2012-01-01T00:52:00.000Z, 2012-01-01T00:54:00.000Z, 20...
```

## TestKit

Apso comes with a TestKit with extra useful matchers for [specs2](https://etorreborre.github.io/specs2/). The following traits with extra matchers are available:

* `CustomMatchers`: provides a matcher to check if an object is serializable and one to check if a file exists;
* `FutureExtraMatchers`: provides extra matchers for futures and implicit conversions for awaitables;
* `JreVersionTestHelper`: provides a wrapper for `AsResult` to only run a spec if a specific JRE version is satisfied;
