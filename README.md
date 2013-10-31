![apso](http://REPOSITORY_URL/apso/raw/master/apso.png)

# Apso

Apso is ShiftForward's utilities library. It provides a series of useful methods:

## Benchmark

The `Benchmark` object provides an apply method to measure the running time of a block of code.

```scala
scala> import eu.shiftforward.apso.Benchmark
import eu.shiftforward.apso.Benchmark

scala> Benchmark("test") { (0 to 100000000).sum }
# Block "test" completed, time taken: 1 ms (0.001 s)
res0: Int = 987459712
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
