![apso](http://REPOSITORY_URL/apso/raw/master/apso.png)

# Apso

Apso is ShiftForward's utilities library. It provides a series of useful methods:

## Benchmark

The `Benchmark` object provides an apply method to measure the running time of a block of code.

```scala
scala> import eu.shiftforward.apso.Benchmark
import eu.shiftforward.apso.Benchmark

scala> Benchmark("test") { (0l to 100000000).sum }
# Block "test" completed, time taken: 1 ms (0.001 s)
res0: Int = 5000000050000000
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

## Implicits

Apso provides implicit conversions from `String`, `Seq[_]`, `Map[_, _]`, `Seq[Map[_, _]]` and `AutoCloseable` to extended types that come packed with extended features.

```scala
scala> import eu.shiftforward.apso.Implicits._
import eu.shiftforward.apso.Implicits._

scala> "abcd".enumerate(2)
res0: IndexedSeq[String] = Vector(aa, ab, ac, ad, ba, bb, bc, bd, ca, cb, cc, cd, da, db, dc, dd)

scala> "abcd".padLeft(10, '0')
res1: String = 000000abcd

scala> "abcd".getBytesWithNullTerminator
res2: Array[Byte] = Array(97, 98, 99, 100, 0)

scala> Seq(1, 2, 3, 4).split(2)
res3: IndexedSeq[Seq[Int]] = Vector(List(1, 2), List(3, 4))

scala> Seq(1, 2, 3, 4, 5).split(2)
res4: IndexedSeq[Seq[Int]] = Vector(List(1, 2, 3), List(4, 5))

scala> Seq(1, 2, 3, 4).sample(0.8)
res5: Seq[Int] = List(1, 2, 3)

scala> Map(1 -> 2, 3 -> 6).merge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
res6: Map[Int,Int] = Map(1 -> 2, 3 -> 5)

scala> Map(1 -> 2, 2 -> 4, 3 -> 6).merge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
res7: Map[Int,Int] = Map(1 -> 2, 2 -> 2, 3 -> 5)

scala> Map(1 -> 2, 3 -> 6).twoWayMerge(Map(2 -> 4, 3 -> 5)) { (a, b) => b }
res8: Map[Int,Int] = Map(2 -> 4, 3 -> 5, 1 -> 2)

scala> Map(1 -> 2, 2 -> 4, 3 -> 6).twoWayMerge(Map(2 -> 2, 3 -> 5)) { (a, b) => b }
res9: Map[Int,Int] = Map(2 -> 2, 3 -> 5, 1 -> 2)

scala> Seq(Map(1 -> 2, 2 -> 4), Map(3 -> 6, 4 -> 8)).sequenceOnMap()
res10: Map[Int,List[Int]] = Map(1 -> List(2), 2 -> List(4), 3 -> List(6), 4 -> List(8))

scala> Seq(Map(1 -> 2, 2 -> 4), Map(3 -> 6, 4 -> 8)).sequenceOnMap(Some(0))
res11: Map[Int,List[Int]] = Map(1 -> List(2, 0), 2 -> List(4, 0), 3 -> List(0, 6), 4 -> List(0, 8))
```

## Logging

The `Logging` trait allows to mixin a slf4j `Logger` object.

```scala
scala> class A extends Logging {}
defined class A

scala> val a = new A
a: A = A@58af6f21

scala> a.log.info("test")
...
```

## ProgressBar

The `ProgressBar` represents a widget to print a dynamic ProgressBar in a console.

```scala
scala> val progress = ProgressBar(100)
progress: eu.shiftforward.apso.ProgressBar = ProgressBar(100)

scala> progress.tick(1)
  1%  / [0.03920492413847179] ops/second

scala> progress.tick(1)
  2% # - [0.8658008658008658] ops/second

scala> progress.tick(1)
  3% # \ [1.0482180293501049] ops/second

scala> progress.tick(1)
  4% ## | [1.152073732718894] ops/second

scala> progress.tick(10)
 14% ####### - [4.22654268808115] ops/second

scala> progress.tick(20)
 34% ################# - [6.546644844517186] ops/second

scala> progress.tick(30)
 64% ################################ | [6.660746003552397] ops/second
```

## Sampler

The `Sampler` class encapsulates a sampling strategy over a sequence of elements. Take into account that the sample is not random.

```scala
scala> val l = (0 until 128).toList
l: List[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127)

scala> val expSampler = ExpSampler[Int]()
expSampler: eu.shiftforward.apso.ExpSampler[Int] = ExpSampler(2.0)

scala> expSampler(1)(l)
res0: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63)

scala> expSampler(2)(l)
res1: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31)

scala> expSampler(3)(l)
res2: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

scala> expSampler(4)(l)
res3: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7)

scala> val listSampler = ListSampler[Int](0.5, 0.2, 0.1)
listSampler: eu.shiftforward.apso.ListSampler[Int] = ListSampler(WrappedArray(0.5, 0.2, 0.1))

scala> listSampler(0)(l)
res4: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63)

scala> listSampler(1)(l)
res5: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)

scala> listSampler(2)(l)
res6: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

scala> val listSamplerWithFallback = new ListSampler[Int](0.5, 0.2, 0.1) with FallbackToMinimum[Int] { val minSample = 0.2 }
listSamplerWithFallback: eu.shiftforward.apso.ListSampler[Int] with eu.shiftforward.apso.FallbackToMinimum[Int] = ListSampler(WrappedArray(0.5, 0.2, 0.1))

scala> listSamplerWithFallback(0)(l)
res7: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63)

scala> listSamplerWithFallback(1)(l)
res8: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)

scala> listSamplerWithFallback(2)(l)
res9: Seq[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
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

## Amazon Web Services

Apso provides a group of classes to ease the interaction with the Amazon Web Services, namely S3 and EC2. Refer to the package `eu.shiftforward.apso.aws` for more details.

## Collections

The `eu.shiftforward.apso.collection` package provides some helpful collections:

* The `DeboxMap` is a `Map` that doesn't box. It is based on the `DeboxMap` at [non/debox](https://github.com/non/debox) but includes a bunch of bugfixes.
* The `HMap` is an implementation of an heterogeneous `Map`.

```scala
scala> import eu.shiftforward.apso.collection._
import eu.shiftforward.apso.collection._

scala> val m = DeboxMap[Int, Int]()
m: eu.shiftforward.apso.collection.DeboxMap[Int,Int] = <function1>

scala> m(1) = 2

scala> m(2) = 3

scala> m(1)
res0: Int = 2

scala> m(2)
res1: Int = 3

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

## Iterators

Apso provides some utility iterators.

```scala
scala> val circularIterator = CircularIterator(List(1, 2, 3).toIterator)
circularIterator: eu.shiftforward.apso.iterator.CircularIterator[Int] = non-empty iterator

scala> circularIterator.take(10).toList
res0: List[Int] = List(1, 2, 3, 1, 2, 3, 1, 2, 3, 1)

scala> val compositeIterator = CompositeIterator(List(1, 2, 3).toIterator, List(4, 5, 6).toIterator, List(7, 8, 9).toIterator)
compositeIterator: eu.shiftforward.apso.iterator.CompositeIterator[Int] = non-empty iterator

scala> compositeIterator.take(9).toList
res1: List[Int] = List(1, 2, 3, 4, 5, 6, 7, 8, 9)

scala> val roundRobinIterator = RoundRobinIterator(List(1, 2, 3).toIterator, List(4, 5, 6).toIterator, List(7, 8, 9).toIterator)
roundRobinIterator: eu.shiftforward.apso.iterator.RoundRobinIterator[Int] = non-empty iterator

scala> roundRobinIterator.take(9).toList
res2: List[Int] = List(1, 4, 7, 2, 5, 8, 3, 6, 9)
```

## JSON

Apso includes a bunch of utilities to work with JSON serialization and deserialization.

### JsonConvert

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

### JsonHMap

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

## Pool

The `Pool` trait provides an object pooling interface.

```scala
scala> import eu.shiftforward.apso.pool._
import eu.shiftforward.apso.pool._

scala> class Foo()
defined class Foo

scala> val pool = UnrestrictedPool(new Foo)
pool: eu.shiftforward.apso.pool.UnrestrictedPool[Foo] = eu.shiftforward.apso.pool.UnrestrictedPool@5ca7751d

scala> val f1 = pool.acquire()
f1: Foo = Foo@2d3827e2

scala> pool.release(f1)

scala> pool.acquire()
res0: Foo = Foo@2d3827e2

scala> pool.acquire()
res1: Foo = Foo@189f417b
```

## Time

The `eu.shiftforward.apso.time` package provides utilities to work with `DateTime`.

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

## Memo

The `Memo` trait (and higher arity friends), provide a way to decorate a function so that it becomes memoized.

```scala
scala> import eu.shiftforward.apso._
import eu.shiftforward.apso._

scala> val square = Memo { x: Int => x * x }
square: eu.shiftforward.apso.Memo[Int,Int] = <function1>

scala> square(2)
res0: Int = 4
```
