![apso](http://REPOSITORY_URL/apso/raw/master/apso.png)

# Apso

Apso is ShiftForward's utilities library. It provides a series of useful methods:

## Benchmark

The `Benchmark` object provides an apply method to measure the running time of a block of code. For example:

```scala
scala> Benchmark("test") { (0 to 100000000).sum }
# Block "test" completed, time taken: 1 ms (0.001 s)
res0: Int = 987459712
```

## CounterPair

The `CounterPair` object provides a method to pack two numbers in the range of an unsigned short in an `Int`. For example:

```scala
scala> CounterPair(1, 2)
res0: Int = 131073

scala> CounterPair(a, b) = res0
a: Int = 1
b: Int = 2
```
