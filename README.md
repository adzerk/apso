# Apso

Apso is ShiftForward's utilities library. It provides a series of useful methods.

## Table of Contents

- [Benchmark](#benchmark)
- [CounterPair](#counterpair)
- [Geo](#geo)
- [JreVersionHelper](#jreversionhelper)
- [Logging](#logging)
- [OrderingHelper](#orderinghelper)
- [ProgressBar](#progressbar)
- [Reflect](#reflect)
- [Retry](#retry)
- [Sampler](#sampler)
    - [ExpSampler](#expsampler)
    - [ListSampler](#listsampler)
    - [FallbackToMinimum](#fallbacktominimum)
- [ShellRun](#shellrun)
- [Retrier](#retrier)
- [Implicits](#implicits)
- [Amazon Web Services](#amazon-web-services)
    - [ConfigCredentialsProvider](#configcredentialsprovider)
    - [CredentialStore](#credentialstore)
    - [EC2](#ec2)
    - [ElasticIP](#elasticip)
    - [InstanceMetadata](#instancemetadata)
    - [S3](#s3)
    - [S3Bucket](#s3bucket)
    - [SerializableAWSCredentials](#serializableawscredentials)
- [Collections](#collections)
    - [HMap](#hmap)
    - [Trie](#trie)
    - [TypedMap](#typedmap)
- [Config](#config)
    - [LazyConfigFactory](#lazyconfigfactory)
    - [Implicits](#implicits)
- [Hashing](#hashing)
- [HTTP](#http)
- [IO](#io)
    - [FileDescriptor](#filedescriptor)
    - [ResourceUtil](#resourceutil)
- [Iterators](#iterators)
    - [CircularIterator](#circulariterator)
    - [CompositeIterator](#compositeiterator)
    - [ExtendedIterator](#extendediterator)
    - [MergedBufferedIterator](#mergedbufferediterator)
    - [RoundRobinIterator](#roundrobiniterator)
- [JSON](#json)
    - [ExtraJsonProtocol](#extrajsonprotocol)
    - [JsValue](#jsvalue)
    - [JsonConvert](#jsonconvert)
    - [JsonFormatBuilder](#jsonformatbuilder)
    - [JsonHMap](#jsonhmap)
- [Profiling](#profiling)
    - [CpuSampler](#cpusampler)
    - [SimpleJmx](#simplejmx)
- [Scalaz](#scalaz)
- [Spray](#spray)
    - [ClientIPDirectives](#clientipdirectives)
    - [ExtraMiscDirectives](#extramiscdirectives)
    - [Implicits](#implicits)
    - [ProxySupport](#proxysupport)
- [Time](#time)
- [TestKit](#testkit)

## Benchmark

The `Benchmark` object provides an apply method to measure the running time of a block of code.

```scala
scala> import eu.shiftforward.apso.Benchmark
import eu.shiftforward.apso.Benchmark

scala> Benchmark("test") { (0l to 100000000).sum }
# Block "test" completed, time taken: 1 ms (0.001 s)
res0: Int = 5000000050000000
```

It's also possible to customize the method (`String => Unit`) that is used to print the results of the benchmark (by default it's `println`). This can be particularly useful if you want to use a logging framework, for example:

```scala
scala> def info(s: String) = println(s"[INFO] $s")
info: (s: String)Unit

scala> Benchmark("test", info) { (0l to 100000000).sum }
[INFO] # Block "test" completed, time taken: 0 ms (0.0 s)
res1: Long = 5000000050000000
```

## CounterPair

The `CounterPair` object provides a method to pack two numbers in the range of an unsigned short in an `Int`.

```scala
scala> import eu.shiftforward.apso.CounterPair
import eu.shiftforward.apso.CounterPair

scala> CounterPair(1, 2)
res0: Int = 131073

scala> CounterPair(a, b) = res0
a: Int = 1
b: Int = 2
```

## Geo

The `Geo` object provides methods to compute distances in kilometers between two points on the planet Earth, calculated using the spherical [law of cosines](https://en.wikipedia.org/wiki/Great-circle_distance#Formulas). Coordinates are represented by a pair of `Double` for latitude and longitude.

```scala
scala> import eu.shiftforward.apso.Geo
import eu.shiftforward.apso.Geo

scala> Geo.distance((41.1617609, -8.6024716), (41.1763745, -8.5964861))
res0: Double = 1.7004440762344684
```

You can also have the distance function curried if you are computing distances from a fixed point:

```scala
scala> val distFromOffice = Geo.distanceFrom((41.1617609, -8.6024716))
distToOffice: eu.shiftforward.apso.Geo.Coordinates => Double = <function1>

scala> distFromOffice((41.1763745, -8.5964861))
res1: Double = 1.7004440762344684

scala> distFromOffice((38.7223032, -9.1414664))
res2: Double = 275.118392477037
```

## JreVersionHelper

The JreVersionHelper object provides helper methods to check the two most significant parts of the JRE version at runtime:

```scala
scala> import eu.shiftforward.apso.JreVersionHelper
import eu.shiftforward.apso.JreVersionHelper

scala> JreVersionHelper.jreVersion
res0: (Int, Int) = (1,8)
```

## Logging

The `Logging` and `StrictLogging` traits allows mixing in slf4j `Logger` objects. The difference between the two is that in the former the `Logger` object is initialized lazily, while in the latter it is initialized strictly:

```scala
scala> class A extends Logging {}
defined class A

scala> val a = new A
a: A = A@58af6f21

scala> a.log.info("test")
...
```

## OrderingHelper

The `OrderingHelper` object provides the `min` and `max` methods for comparing two instances of the same type:

```scala
scala> import eu.shiftforward.apso.OrderingHelper._
import eu.shiftforward.apso.OrderingHelper._

scala> min(2, 3)
res0: Int = 2

scala> max(2, 3)
res1: Int = 3
```

## ProgressBar

The `ProgressBar` represents a widget to print a dynamic progress bar in a console.

```scala
scala> import eu.shiftforward.apso.ProgressBar
import eu.shiftforward.apso.ProgressBar

scala> val progress = ProgressBar(100)
progress: eu.shiftforward.apso.ProgressBar = ProgressBar(100)

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
scala> import eu.shiftforward.apso.Reflect
import eu.shiftforward.apso.Reflect

scala> import eu.shiftforward.apso.collection._
import eu.shiftforward.apso.collection._

scala> Reflect.newInstance[HMap[Nothing]]("eu.shiftforward.apso.collection.HMap")
res0: eu.shiftforward.apso.collection.HMap[Nothing] = HMap()

scala> Reflect.companion[Reflect.type]("eu.shiftforward.apso.Reflect")
res1: eu.shiftforward.apso.Reflect.type = eu.shiftforward.apso.Reflect$@3b1dbca
```

## Retry

The `Retry` object provides a method to retry a given `Future` a given number of times until it succeeds or the specified maximum number of retries is reached:

```scala
scala> import eu.shiftforward.apso.Retry
import eu.shiftforward.apso.Retry

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

scala> Retry(10)(f).onComplete(println)
Success(6)
```

## Sampler

The `Sampler` trait exposes a method to extract the first elements of a sequence given a sampling level. The sampling level is an integer and should map to a percentage.

The following implementations of `Sampler` are available:

### ExpSampler

The `ExpSampler` is a sampler in which sampling level ratios are distributed in an exponential way. Each sampling level corresponds to a sample with `1.0 / pow(base, level)` of the original size:

```scala
scala> import eu.shiftforward.apso.ExpSampler
import eu.shiftforward.apso.ExpSampler

scala> val s = ExpSampler[Int](2)
s: eu.shiftforward.apso.ExpSampler[Int] = ExpSampler(2.0)

scala> s(0)((0 to 15).toSeq)
res0: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

scala> s(1)((0 to 15).toSeq)
res1: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7)

scala> s(2)((0 to 15).toSeq)
res2: Seq[Int] = Range(0, 1, 2, 3)

scala> s(3)((0 to 15).toSeq)
res3: Seq[Int] = Range(0, 1)

scala> s(4)((0 to 15).toSeq)
res4: Seq[Int] = Range(0)
```

### ListSampler

The `ListSampler` is a sampler in which the ratios for each sampling level are given explicitly as parameters:

```scala
scala> import eu.shiftforward.apso.ListSampler
import eu.shiftforward.apso.ListSampler

scala> val s = ListSampler[Int](0.5, 0.25, 0.125, 0.0625)
s: eu.shiftforward.apso.ListSampler[Int] = ListSampler(WrappedArray(0.5, 0.25, 0.125, 0.0625))

scala> s(0)((0 to 15).toSeq)
res0: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7)

scala> s(1)((0 to 15).toSeq)
res1: Seq[Int] = Range(0, 1, 2, 3)

scala> s(2)((0 to 15).toSeq)
res2: Seq[Int] = Range(0, 1)

scala> s(3)((0 to 15).toSeq)
res3: Seq[Int] = Range(0)
```

### FallbackToMinimum

The `FallbackToMinimum` trait allows one to set a minimum ratio for any sampling level:

```scala
scala> import eu.shiftforward.apso.ExpSampler
import eu.shiftforward.apso.ExpSampler

scala> import eu.shiftforward.apso.FallbackToMinimum
import eu.shiftforward.apso.FallbackToMinimum

scala> val s = new ExpSampler[Int](2) with FallbackToMinimum[Int] { val minSample = 0.5 }
s: eu.shiftforward.apso.ExpSampler[Int] with eu.shiftforward.apso.FallbackToMinimum[Int] = ExpSampler(2.0)

scala> s(0)((0 to 15).toSeq)
res0: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

scala> s(1)((0 to 15).toSeq)
res1: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7)

scala> s(2)((0 to 15).toSeq)
res2: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7)

scala> s(3)((0 to 15).toSeq)
res3: Seq[Int] = Range(0, 1, 2, 3, 4, 5, 6, 7)
```

## ShellRun

The `ShellRun` object wraps the Scala's process library, facilitating the launching of shell commands.

```scala
scala> ShellRun("ls")
res0: String =
"CHANGELOG
README.md
apso
apso-testkit
apso.png
project
sbt
target
"

scala> ShellRun("ls", "-l")
res1: String =
"total 248
-rw-r--r--  1 jcazevedo  staff   5190 Oct 30 17:52 CHANGELOG
-rw-r--r--  1 jcazevedo  staff   6579 Oct 31 11:44 README.md
drwxr-xr-x  4 jcazevedo  staff    136 Oct 30 18:14 apso
drwxr-xr-x  4 jcazevedo  staff    136 Oct 30 18:15 apso-testkit
-rw-r--r--@ 1 jcazevedo  staff  91715 Oct 30 17:43 apso.png
drwxr-xr-x  6 jcazevedo  staff    204 Oct 30 18:20 project
-rwxr-xr-x  1 jcazevedo  staff  16035 Sep 26 14:04 sbt
drwxr-xr-x  8 jcazevedo  staff    272 Oct 30 18:14 target
"
```

## Retrier

The `Retrier` is a helper class for actors that need to retry some of the messages they send to other actors until a certain acknowledgement message (ACK) is received. Messages can be sent individually or in batches.

This class is instantiated by providing functions that extract an identifier from sent messages and from ACK messages. This can be an arbitrary identifier, as long as it uniquely associates a received ACK with the original sent message. Optional per-message filtering functions can be given, as well as the frequency of the retries and an optional timeout. Finally, the `onComplete` method, which is executed after a message or group of messages is acknowledged, must be implemented.

A `Retrier` can be used as follows:

```scala
case class ChangeData(reqId: Long, data: String)
case class ChangeDataAck(reqId: Long)
case class Replicate(reqId: Long, data: String)
case class ReplicateAck(reqId: Long)

class Master(val replica: ActorRef) extends Actor {
  import Retrier._

  val retrier = new Retrier[(ActorRef, ChangeData), Replicate, ReplicateAck, Long](_.reqId, _.reqId) {
    def onComplete(req: (ActorRef, ChangeData)) = req._1 ! ChangeDataAck(req._2.reqId)
  }

  def receive: Receive = ({
    case msg @ ChangeData(reqId, data) =>
      // change data internally here
      retrier.dispatch((sender, msg), Replicate(reqId, data), replica)
    }: Receive).orRetryWith(retrier)
}
```

In the previous example, every time a `Master` actor receives a `ChangeData` message, it sends a `Replicate` message to a replica actor and only responds to the original sender after an acknowledgement from the replica is received. The `Replicate` message is retried periodically.

## Implicits

Apso provides implicit conversions from `String`, `Seq[_]`, `Map[_, _]`, `Seq[Map[_, _]]` and `AutoCloseable` to extended types that come packed with extended features.

```scala
scala> import eu.shiftforward.apso.Implicits._
import eu.shiftforward.apso.Implicits._

scala> "abcd".some
res0: Some[String] = Some(abcd)

scala> "abcd".enumerate(2)
res1: IndexedSeq[String] = Vector(aa, ab, ac, ad, ba, bb, bc, bd, ca, cb, cc, cd, da, db, dc, dd)

scala> "abcd".padLeft(10, '0')
res2: String = 000000abcd

scala> "abcd".getBytesWithNullTerminator
res3: Array[Byte] = Array(97, 98, 99, 100, 0)

scala> Seq(1, 2, 3, 4).split(2)
res4: IndexedSeq[Seq[Int]] = Vector(List(1, 2), List(3, 4))

scala> Seq(1, 2, 3, 4, 5).split(2)
res5: IndexedSeq[Seq[Int]] = Vector(List(1, 2, 3), List(4, 5))

scala> Seq(1, 2, 3, 4).sample(0.8)
res6: Seq[Int] = List(1, 2, 3)

scala> scala.util.Random.shuffle((0 to 15)).takeSmallest(3)
res7: Seq[Int] = List(0, 1, 2)

scala> scala.util.Random.shuffle((0 to 15)).takeLargest(3)
res8: Seq[Int] = List(15, 14, 13)

scala> Seq(1, 3, 5).mergeSorted(Seq(2, 4))
res9: Seq[Int] = List(1, 2, 3, 4, 5)

scala> (0 to 15).average
res10: Int = 7

scala> Iterator(1, 3, 5).buffered.mergeSorted(Iterator(2, 4).buffered).toList
res11: List[Int] = List(1, 2, 3, 4, 5)

scala> val it = Iterator(1, 2, 3, 4, 5, 6, 7, 8).buffered
it: scala.collection.BufferedIterator[Int] = non-empty iterator

scala> it.bufferedTakeWhile(_ < 4).toList
res12: List[Int] = List(1, 2, 3)

scala> it.toList
res13: List[Int] = List(4, 5, 6, 7, 8)

scala> Map(1 -> 2, 3 -> 6).merge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
res14: Map[Int,Int] = Map(1 -> 2, 3 -> 5)

scala> Map(1 -> 2, 2 -> 4, 3 -> 6).merge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
res15: Map[Int,Int] = Map(1 -> 2, 2 -> 2, 3 -> 5)

scala> Map(1 -> 2, 3 -> 6).twoWayMerge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
res16: Map[Int,Int] = Map(2 -> 4, 3 -> 5, 1 -> 2)

scala> Map(1 -> 2, 2 -> 4, 3 -> 6).twoWayMerge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
res17: Map[Int,Int] = Map(2 -> 2, 3 -> 5, 1 -> 2)

scala> Map(1 -> 2, 2 -> 3).mapKeys(_ + 1)
res18: Map[Int,Int] = Map(2 -> 2, 3 -> 3)

scala> Seq(Map(1 -> 2, 2 -> 4), Map(3 -> 6, 4 -> 8)).sequenceOnMap()
res19: Map[Int,List[Int]] = Map(1 -> List(2), 2 -> List(4), 3 -> List(6), 4 -> List(8))

scala> Seq(Map(1 -> 2, 2 -> 3), Map(1 -> 3), Map(2 -> 4, 3 -> 5)).sequenceOnMap()
res20: Map[Int,List[Int]] = Map(1 -> List(2, 3), 2 -> List(3, 4), 3 -> List(5))

scala> Seq(Map(1 -> 2, 2 -> 4), Map(3 -> 6, 4 -> 8)).sequenceOnMap(zero = Some(0))
res21: Map[Int,List[Int]] = Map(1 -> List(2, 0), 2 -> List(4, 0), 3 -> List(0, 6), 4 -> List(0, 8))

scala> Future.successful[Option[Int]](None).ifNoneOrErrorFallbackTo(Future.successful[Option[Int]](Some(4))).onComplete(println)
Success(Some(4))

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
scala> import eu.shiftforward.apso.aws._
import eu.shiftforward.apso.aws._

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
confProvider: eu.shiftforward.apso.aws.ConfigCredentialsProvider = ConfigCredentialsProvider(Config(SimpleConfigObject({"aws":{"access-key":"<access-key>","secret-key":"<secret-key>"}})),aws.access-key,aws.secret-key)

scala> val credentials = confProvider.getCredentials
credentials: com.amazonaws.auth.AWSCredentials = com.amazonaws.auth.BasicAWSCredentials@46056cef

scala> credentials.getAWSAccessKeyId
res0: String = <access-key>

scala> credentials.getAWSSecretKey
res1: String = <secret-key>
```

### CredentialStore

The `CredentialStore` object serves as an endpoint for the retrieval of AWS credentials from available configurations. It extends the chain in the `DefaultAWSCredentialsProviderChain` (from AWS SDK for Java) with the retrieval of AWS credentials through the default typesafe configuration file (typically `application.conf`).

### EC2

The `EC2` class wraps an instance of `AmazonEC2` (from AWS SDK for Java), providing a higher level interface for querying the currently running instances. It provides methods to easily access an instance by its `id`, as well as listing all currently running instances. A method to easily terminate an instance given its `id` is also provided. The `EC2` object provides an implicit conversion of an `Instance` to a `RichEC2Instance`, that enables the usage of methods `id` (to return the id of an instance) and `tagValue(key)` (to return the value of a tag).

### ElasticIP

The `ElasticIP` class provides a representation of an AWS elastic IP address. It publishes the method `associateTo(instance)` to allow easier association to an EC2 instance.

### InstanceMetadata

The `InstanceMetadata` object provides utilities for obtaining metadata about the EC2 instance the current process is running on. The methods are not expected to work if the JVM is not running on an EC2 instance.

### S3

The `S3` class wraps an instance of `AmazonS3Client` (from AWS SDK for Java), providing a higher level interface for querying information about buckets and their objects. It publishes methods to easily list the buckets under the object's credentials, as well as list the objects in a bucket, filtered by an optional prefix.

### S3Bucket

The `S3Bucket` class wraps an instance of `AmazonS3Client` (from AWS SDK for Java) and exposes a higher level interface for pushing and pulling files to and from a bucket.

### SerializableAWSCredentials

The `SerializableAWSCredentials` class provides a serializable container for AWS credentials, extending the `AWSCredentials` class (from AWS SDK for Java).

## Collections

The `eu.shiftforward.apso.collection` package provides some helpful collections:

### HMap

The `HMap` is an implementation of an heterogeneous `Map`, where you declare the instances for the keys explicitly:

```scala
scala> import eu.shiftforward.apso.collection._
import eu.shiftforward.apso.collection._

scala> val Key1 = new HMapKey[Int]
Key1: eu.shiftforward.apso.collection.HMapKey[Int] = eu.shiftforward.apso.collection.HMapKey@4eb14055

scala> val Key2 = new HMapKey[String]
Key2: eu.shiftforward.apso.collection.HMapKey[String] = eu.shiftforward.apso.collection.HMapKey@13590b1e

scala> val Key3 = new HMapKey[List[Boolean]]
Key3: eu.shiftforward.apso.collection.HMapKey[List[Boolean]] = eu.shiftforward.apso.collection.HMapKey@7e384bb6

scala> val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))
map: eu.shiftforward.apso.collection.HMap[eu.shiftforward.apso.collection.HMapKey] = HMap((eu.shiftforward.apso.collection.HMapKey@4eb14055,4), (eu.shiftforward.apso.collection.HMapKey@13590b1e,s), (eu.shiftforward.apso.collection.HMapKey@7e384bb6,List(false, true)))

scala> map(Key1)
res2: Int = 4

scala> map(Key2)
res3: String = s

scala> map(Key3)
res4: List[Boolean] = List(false, true)
```

### Trie

The `Trie` class is an implementation of an immutable trie. An example usage follows:

```scala
scala> import eu.shiftforward.apso.collection._
import eu.shiftforward.apso.collection._

scala> val t = Trie[Char, Int]()
t: eu.shiftforward.apso.collection.Trie[Char,Int] = Trie(None,Map())

scala> val nt = t.set("one", 1).set("two", 2).set("three", 3).set("four", 4)
nt: eu.shiftforward.apso.collection.Trie[Char,Int] = Trie(None,Map(o -> Trie(None,Map(n -> Trie(None,Map(e -> Trie(Some(1),Map()))))), t -> Trie(None,Map(w -> Trie(None,Map(o -> Trie(Some(2),Map()))), h -> Trie(None,Map(r -> Trie(None,Map(e -> Trie(None,Map(e -> Trie(Some(3),Map()))))))))), f -> Trie(None,Map(o -> Trie(None,Map(u -> Trie(None,Map(r -> Trie(Some(4),Map())))))))))

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
m: eu.shiftforward.apso.collection.TypedMap[Any] = Map(java.lang.String -> one, Int -> 2, Long -> 3)

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

### Implicits

The `config.Implicits` object allows one to deserialize a config to a type which has a `ConfigReader` implicit in scope. Most of scala's standard library types already have a `ConfigReader` implemented. You can also implement your own `ConfigReaders`. See the following for an example usage:

```scala
scala> import com.typesafe.config._
import com.typesafe.config._

scala> import eu.shiftforward.apso.config.ConfigReader.BasicConfigReaders._
import eu.shiftforward.apso.config.ConfigReader.BasicConfigReaders._

scala> import eu.shiftforward.apso.config.Implicits._
import eu.shiftforward.apso.config.Implicits._

scala> import eu.shiftforward.apso.config._
import eu.shiftforward.apso.config._

scala> import scala.concurrent.duration._
import scala.concurrent.duration._

scala> val conf = ConfigFactory.parseString("""{
     |   v1 = 2
     |   v2 = 60s
     |   v3 = "test"
     |   v4 {
     |     a = 2
     |     b = 3
     |   }
     | }""")
conf: com.typesafe.config.Config = Config(SimpleConfigObject({"v1":2,"v2":"60s","v3":"test","v4":{"a":2,"b":3}}))

scala> conf.get[Int]("v1")
res0: Int = 2

scala> conf.get[FiniteDuration]("v2")
res1: scala.concurrent.duration.FiniteDuration = 1 minute

scala> conf.get[String]("v3")
res2: String = test

scala> case class Foo(a: Int, b: Int)
defined class Foo

scala> implicit val fooConfigReader = new ConfigReader[Foo] {
     |   def apply(config: Config, key: String): Foo = {
     |     val conf = config.get[Config](key)
     |     Foo(conf.get[Int]("a"), conf.get[Int]("b"))
     |   }
     | }
fooConfigReader: eu.shiftforward.apso.config.ConfigReader[Foo] = <function2>

scala> conf.get[Foo]("v4")
res3: Foo = Foo(2,3)
```

## Hashing

Apso provides utilities for various hashing functions.

```scala
scala> import eu.shiftforward.apso.hashing.Implicits._
import eu.shiftforward.apso.hashing.Implicits._

scala> "abcd".md5
res0: String = e2fc714c4727ee9395f324cd2e7f331f

scala> "abcd".murmurHash
res1: Long = 7785666560123423118
```

## HTTP

Apso provides a tiny wrapper for [Dispatch](http://dispatch.databinder.net/) with synchronous operations. It's called `W`, and the following shows some sample usage:

```scala
scala> import eu.shiftforward.apso.http.W
import eu.shiftforward.apso.http.W

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
scala> import eu.shiftforward.apso.io.ResourceUtil
import eu.shiftforward.apso.io.ResourceUtil

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
scala> import eu.shiftforward.apso.iterator.CircularIterator
import eu.shiftforward.apso.iterator.CircularIterator

scala> val circularIterator = CircularIterator(List(1, 2, 3).toIterator)
circularIterator: eu.shiftforward.apso.iterator.CircularIterator[Int] = non-empty iterator

scala> circularIterator.take(10).toList
res0: List[Int] = List(1, 2, 3, 1, 2, 3, 1, 2, 3, 1)
```

### CompositeIterator

The `CompositeIterator` is an iterator that wraps a list of other iterators and iterates over its elements sequentially. It handles compositions of a large number of iterators in a more efficient way than simply concatenating them, avoiding stack overflows in particular. It supports appending of new iterators while keeping its efficiency. See the following for sample usage:

```scala
scala> import eu.shiftforward.apso.iterator.CompositeIterator
import eu.shiftforward.apso.iterator.CompositeIterator

scala> val compositeIterator = CompositeIterator(List(1, 2, 3).toIterator, List(4, 5, 6).toIterator, List(7, 8, 9).toIterator)
compositeIterator: eu.shiftforward.apso.iterator.CompositeIterator[Int] = non-empty iterator

scala> compositeIterator.take(9).toList
res0: List[Int] = List(1, 2, 3, 4, 5, 6, 7, 8, 9)
```

### ExtendedIterator

The `ExtendedIterator` is a decorator for iterators, adding more control over its lifetime. See the following for sample usage:

```scala
scala> import eu.shiftforward.apso.iterator.ExtendedIterator
import eu.shiftforward.apso.iterator.ExtendedIterator

scala> val it = (0 to 15).toIterator
it: Iterator[Int] = non-empty iterator

scala> val extIt = new ExtendedIterator(it)
extIt: eu.shiftforward.apso.iterator.ExtendedIterator[Int] = non-empty iterator

scala> extIt.onEnd(println("finished"))

scala> extIt.length
finished
res1: Int = 16
```

### MergedBufferedIterator

The `MergedBufferedIterator` is a collection of sorted `BufferedIterators` that allows traversing them in order, while also providing a `mergeSorted` method to merge with another sorted `BufferedIterator`. See the following for sample usage:

```scala
scala> import eu.shiftforward.apso.iterator.MergedBufferedIterator
import eu.shiftforward.apso.iterator.MergedBufferedIterator

scala> val it1 = MergedBufferedIterator(List(
     |   (0 to 3).toIterator.buffered,
     |   (0 to 8).toIterator.buffered,
     |   (0 to 15).toIterator.buffered,
     |   (0 to 11).toIterator.buffered))
it1: eu.shiftforward.apso.iterator.MergedBufferedIterator[Int] = non-empty iterator

scala> it1.toList
res0: List[Int] = List(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 10, 10, 11, 11, 12, 13, 14, 15)

scala> val it2 = MergedBufferedIterator(List(
     |   Iterator(1, 3, 5).buffered,
     |   Iterator(2).buffered))
it2: eu.shiftforward.apso.iterator.MergedBufferedIterator[Int] = non-empty iterator

scala> it2.mergeSorted(Iterator(4, 6).buffered).toList
res1: List[Int] = List(1, 2, 3, 4, 5, 6)
```

### RoundRobinIterator

The `RoundRobinIterator` is an iterator that wraps an array of other iterators and iterates over its elements in a round-robin way. See the following for sample usage:

```scala
scala> import eu.shiftforward.apso.iterator.RoundRobinIterator
import eu.shiftforward.apso.iterator.RoundRobinIterator

scala> val roundRobinIterator = RoundRobinIterator(List(1, 2, 3).toIterator, List(4, 5, 6).toIterator, List(7, 8, 9).toIterator)
roundRobinIterator: eu.shiftforward.apso.iterator.RoundRobinIterator[Int] = non-empty iterator

scala> roundRobinIterator.take(9).toList
res0: List[Int] = List(1, 4, 7, 2, 5, 8, 3, 6, 9)
```

## JSON

Apso includes a bunch of utilities to work with JSON serialization and deserialization.

### ExtraJsonProtocol

The `ExtraJsonProtocol` object combines three traits that provide extra `JsonFormats` (of [spray-json](https://github.com/spray/spray-json)) for some relevant types. The `JsonFormats` that are provided en each trait are the following:

* ExtraTimeJsonProtocol: `JsonFormat[FiniteDuration]` and `JsonFormat[Interval]`;
* ExtraHttpJsonProtocol: `JsonFormat[URI]`;
* ExtraMiscJsonProtocol: `JsonFormat[Config]`, `JsonFormat[DateTime]` and `JsonFormat[LocalDate]`.

### JsValue

The `json` package provides some implicits around [spray-json](https://github.com/spray/spray-json)'s `JsValue` to unwrap JSON values, merge two `JsValues` and create `JsValues` from a sequence of dot-separated paths with the corresponding leaf values. See the following for sample usage:

```scala
scala> import eu.shiftforward.apso.json.Implicits._
import eu.shiftforward.apso.json.Implicits._

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
scala> import eu.shiftforward.apso.json._
import eu.shiftforward.apso.json._

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
scala> import eu.shiftforward.apso.json._
import eu.shiftforward.apso.json._

scala> import shapeless._
import shapeless._

scala> import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol._

scala> import spray.json._
import spray.json._

scala> case class Test(a: Int, b: List[String], c: Double)
defined class Test

scala> val builder = JsonFormatBuilder().field[Int]("a").field[List[String]]("b")
builder: eu.shiftforward.apso.json.JsonFormatBuilder[shapeless.::[Int,shapeless.::[List[String],shapeless.HNil]],shapeless.::[eu.shiftforward.apso.json.JsonFormatBuilder.Field[Int],shapeless.::[eu.shiftforward.apso.json.JsonFormatBuilder.Field[List[String]],shapeless.HNil]]] = JsonFormatBuilder(Field(a,spray.json.BasicFormats$IntJsonFormat$@50ef5db8,None) :: Field(b,spray.json.CollectionFormats$$anon$1@4bc54f85,None) :: HNil)

scala> val jf1 = builder.jsonFormat[Test](
     |   { case a :: b :: HNil => Test(a, b, -1.0) },
     |   { test => test.a :: test.b :: HNil })
jf1: spray.json.RootJsonFormat[Test] = eu.shiftforward.apso.json.JsonFormatBuilder$$anon$1@70ae62e7

scala> """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf1)
res0: Test = Test(3,List(x, y),-1.0)

scala> """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf1)
res1: Test = Test(3,List(x, y),-1.0)

scala> Test(3, List("x", "y"), -1.0).toJson(jf1)
res2: spray.json.JsValue = {"b":["x","y"],"a":3}

scala> val builder2 = builder.field[Double]("c", 0.0)
builder2: eu.shiftforward.apso.json.JsonFormatBuilder[shapeless.::[Int,shapeless.::[List[String],shapeless.::[Double,shapeless.HNil]]],shapeless.::[eu.shiftforward.apso.json.JsonFormatBuilder.Field[Int],shapeless.::[eu.shiftforward.apso.json.JsonFormatBuilder.Field[List[String]],shapeless.::[eu.shiftforward.apso.json.JsonFormatBuilder.Field[Double],shapeless.HNil]]]] = JsonFormatBuilder(Field(a,spray.json.BasicFormats$IntJsonFormat$@50ef5db8,None) :: Field(b,spray.json.CollectionFormats$$anon$1@4bc54f85,None) :: Field(c,spray.json.BasicFormats$DoubleJsonFormat$@73930900,Some(0.0)) :: HNil)

scala> val jf2 = builder2.jsonFormat[Test](
     |   { case a :: b :: c :: HNil => Test(a, b, c) },
     |   { test => test.a :: test.b :: test.c :: HNil })
jf2: spray.json.RootJsonFormat[Test] = eu.shiftforward.apso.json.JsonFormatBuilder$$anon$1@6ce596d9

scala> """{ "a": 3, "b": ["x", "y"] }""".parseJson.convertTo[Test](jf2)
res3: Test = Test(3,List(x, y),0.0)

scala> """{ "a": 3, "b": ["x", "y"], "c": 3.0 }""".parseJson.convertTo[Test](jf2)
res4: Test = Test(3,List(x, y),3.0)

scala> Test(3, List("x", "y"), 0.0).toJson(jf2)
res5: spray.json.JsValue = {"c":0.0,"b":["x","y"],"a":3}
```

### JsonHMap

The `JsonHMap` defines an heterogeneous map with JSON (de)serialization capabilities. See the following for sample usage:

```scala
scala> import spray.json._
import spray.json._

scala> import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol._

scala> import eu.shiftforward.apso.json._
import eu.shiftforward.apso.json._

scala> import eu.shiftforward.apso.json.JsonHMap._
import eu.shiftforward.apso.json.JsonHMap._

scala> import eu.shiftforward.apso.collection._
import eu.shiftforward.apso.collection._

scala> implicit val reg = new JsonKeyRegistry {}
reg: eu.shiftforward.apso.json.JsonKeyRegistry = $anon$1@4213d40

scala> val Key1 = new JsonHMapKey[Int]('key1) {}
Key1: eu.shiftforward.apso.json.JsonHMapKey[Int] = 'key1

scala> val Key2 = new JsonHMapKey[String]('key2) {}
Key2: eu.shiftforward.apso.json.JsonHMapKey[String] = 'key2

scala> val Key3 = new JsonHMapKey[List[Boolean]]('key3) {}
Key3: eu.shiftforward.apso.json.JsonHMapKey[List[Boolean]] = 'key3

scala> val json =
         """
           |{
           |  "key1": 4,
           |  "key2": "s",
           |  "key3": [ false, true ]
           |}""".stripMargin
json: String =
"
{
  "key1": 4,
  "key2": "s",
  "key3": [ false, true ]
}"

scala> val map = json.asJson.convertTo[JsonHMap]
map: eu.shiftforward.apso.json.JsonHMap.JsonHMap = HMap(('key3,List(false, true)), ('key2,s), ('key1,4))
```

## Profiling

The `profiling` package of apso provides utilities to help with profiling the running process.

### CpuSampler

The `CpuSampler` is a lightweight configurable CPU profiler based on call stack sampling. When run as a thread, it periodically captures the call stacks of all live threads and maintains counters for each leaf method. The counters are then dumped to a logger with a given periodicity (most probably greater than the sampling period). Each data row written to the logger contains a timestamp, the method profiled, its location in the source code and the associated absolute counters and relative weight.

### SimpleJmx

The `SimpleJmx` trait allows mixing in a simple JMX server. The JMX server is configured through a `Config` object, where the parameters `host` and `port` can be set. When behind a firewall, both the `port` defined (the RMI registry port) and the `port + 1` port (the RMI server port) need to be open. In the event of a binding failure to the defined port, a retry is performed with a random port.

## Scalaz

The `scalaz` package provides implicit methods to convert between Scala's `Try` and Scalaz's `Validation`. See the following for a sample usage:

```scala
scala> import eu.shiftforward.apso.scalaz.Implicits._
import eu.shiftforward.apso.scalaz.Implicits._

scala> import scala.util._
import scala.util._

scala> import scalaz._
import scalaz._

scala> Try(2): Validation[Throwable, Int]
res0: scalaz.Validation[Throwable,Int] = Success(2)

scala> Try(throw new Exception()): Validation[Throwable, Int]
res1: scalaz.Validation[Throwable,Int] = Failure(java.lang.Exception)
```

## Spray

The `spray` package provides additional [directives](http://spray.io/documentation/1.2.2/spray-routing/key-concepts/directives/) to be used in [spray-routing](https://github.com/spray/spray).

### ClientIPDirectives

The `ClientIPDirectives` trait exposes an `optionalRawClientIP` directive that extracts the raw IP of the client from either the `X-Forwarded-For`, `Remote-Address` or `X-Real-IP` header, in that order of priority.

### ExtraMiscDirectives

The `ExtraMiscDirectives` trait exposes the directives `cacheControlMaxAge(inMinutes)` and `optionalRefererHost` to set the cache-control header to the supplied number of minutes and to extract the referer from the HTTP request header, respectively. The `ExtraMiscDirectives` companion object exposes a `cacheControlNoCache` directive to reply with the `no-cache` option in the `Cache-Control` header.

### Implicits

The `Implicits` companion object exposes an implicit method that provides a [`Marshaller`](http://spray.io/documentation/1.2.2/spray-httpx/marshalling/) for Scalaz's `Validation`.

### ProxySupport

The `ProxySupport` traits adds helper methods to proxy requests to a given uri, either directly (`proxyTo`), or with the unmatched path and query parameters of the current context (`proxyToUnmatchedPath`).

## Time

The `eu.shiftforward.apso.time` package provides utilities to work with `DateTime` and `LocalDate`. It mainly adds support for better working with intervals. See the following sample usage:

```scala
scala> import com.github.nscala_time.time.Imports._
import com.github.nscala_time.time.Imports._

scala> import eu.shiftforward.apso.time._
import eu.shiftforward.apso.time._

scala> import eu.shiftforward.apso.time.Implicits._
import eu.shiftforward.apso.time.Implicits._

scala> (new DateTime("2012-01-01") to new DateTime("2012-01-01")).toList
res0: List[com.github.nscala_time.time.Imports.DateTime] = List(2012-01-01T00:00:00.000Z)

scala> (new DateTime("2012-02-01") until new DateTime("2012-03-01") by 1.day)
res1: eu.shiftforward.apso.time.IterableInterval = SteppedInterval(2012-02-01T00:00:00.000Z, 2012-02-02T00:00:00.000Z, 2012-02-03T00:00:00.000Z, 2012-02-04T00:00:00.000Z, 2012-02-05T00:00:00.000Z, 2012-02-06T00:00:00.000Z, 2012-02-07T00:00:00.000Z, 2012-02-08T00:00:00.000Z, 2012-02-09T00:00:00.000Z, 2012-02-10T00:00:00.000Z, 2012-02-11T00:00:00.000Z, 2012-02-12T00:00:00.000Z, 2012-02-13T00:00:00.000Z, 2012-02-14T00:00:00.000Z, 2012-02-15T00:00:00.000Z, 2012-02-16T00:00:00.000Z, 2012-02-17T00:00:00.000Z, 2012-02-18T00:00:00.000Z, 2012-02-19T00:00:00.000Z, 2012-02-20T00:00:00.000Z, 2012-02-21T00:00:00.000Z, 2012-02-22T00:00:00.000Z, 2012-02-23T00:00:00.000Z, 2012-02-24T00:00:00.000Z, 2012-02-25T00:00:00.000Z, 2012-02-26T00:00:00.000Z, 2012-02-27T00:00:00.000Z, 2012-02-28T00:00:00.000Z, 20...

scala> (new DateTime("2012-01-01") until new DateTime("2012-02-01") by 2.minutes)
res2: eu.shiftforward.apso.time.IterableInterval = SteppedInterval(2012-01-01T00:00:00.000Z, 2012-01-01T00:02:00.000Z, 2012-01-01T00:04:00.000Z, 2012-01-01T00:06:00.000Z, 2012-01-01T00:08:00.000Z, 2012-01-01T00:10:00.000Z, 2012-01-01T00:12:00.000Z, 2012-01-01T00:14:00.000Z, 2012-01-01T00:16:00.000Z, 2012-01-01T00:18:00.000Z, 2012-01-01T00:20:00.000Z, 2012-01-01T00:22:00.000Z, 2012-01-01T00:24:00.000Z, 2012-01-01T00:26:00.000Z, 2012-01-01T00:28:00.000Z, 2012-01-01T00:30:00.000Z, 2012-01-01T00:32:00.000Z, 2012-01-01T00:34:00.000Z, 2012-01-01T00:36:00.000Z, 2012-01-01T00:38:00.000Z, 2012-01-01T00:40:00.000Z, 2012-01-01T00:42:00.000Z, 2012-01-01T00:44:00.000Z, 2012-01-01T00:46:00.000Z, 2012-01-01T00:48:00.000Z, 2012-01-01T00:50:00.000Z, 2012-01-01T00:52:00.000Z, 2012-01-01T00:54:00.000Z, 20...
```

## TestKit

Apso comes with a TestKit with extra useful matchers for [specs2](https://etorreborre.github.io/specs2/). The following traits with extra matchers are available:

* `CustomMatchers`: provides a matcher to check if an object is serializable and one to check if a file exists;
* `FutureExtraMatchers`: provides extra matchers for futures and implicit conversions for awaitables;
* `JreVersionTestHelper`: provides a wrapper for `AsResult` to only run a spec if a specific JRE version is satisfied;
* `TestHelper`: provides a helper method to create a temporary directory that is deleted on exit;
* `ActorMatchers`: provides various matchers to be used on akka's TestKit probes and check for different behaviours regarding the reception of messages.
