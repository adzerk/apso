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
