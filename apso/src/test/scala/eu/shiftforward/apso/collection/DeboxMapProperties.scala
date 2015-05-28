package eu.shiftforward.apso.collection

import org.scalacheck.{ Gen, Arbitrary, Properties }
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll

import scala.reflect.ClassTag

class DeboxMapProperties extends Properties("DeboxMap") {
  type K = String
  type V = Int

  def genDeboxMap[A: Arbitrary: ClassTag, B: Arbitrary: ClassTag]: Gen[DeboxMap[A, B]] = {
    import org.scalacheck.Gen._
    for {
      keys <- listOf(arbitrary[A])
      values <- listOfN(keys.size, arbitrary[B])
      removedKeys <- someOf(keys)
    } yield if (keys.isEmpty)
      DeboxMap.empty[A, B]
    else {
      val map = DeboxMap[A, B](keys.toArray, values.toArray)
      // Remove some keys to have invalid blocks
      removedKeys.foreach { k => map.remove(k) }
      map
    }
  }

  implicit def arbDeboxMap[A: Arbitrary: ClassTag, B: Arbitrary: ClassTag] = Arbitrary(genDeboxMap[A, B])

  property("get, contains, equals") = forAll { (entries: Map[K, V]) =>
    val map = DeboxMap.empty[K, V]
    entries.foreach { case (k, v) => map.update(k, v) }
    entries.forall {
      case (k, v) =>
        map.contains(k) == entries.contains(k) &&
          map.get(k) == entries.get(k)
    } && map.equals(map) && !map.equals(0)
  }

  property("update") = forAll { (map: DeboxMap[K, V], k: K, v: V) =>
    map.update(k, v)
    map.contains(k) && map.get(k) == Some(v)
  }

  property("remove") = forAll { (map: DeboxMap[K, V], k: K) =>
    map.remove(k)
    !map.contains(k) && map.get(k) == None
  }

  property("remove, length") = forAll { (map: DeboxMap[K, V], k: K) =>
    val initialLength = map.length
    if (map.contains(k)) {
      map.remove(k)
      map.length == initialLength - 1
    } else {
      map.remove(k)
      map.length == initialLength
    }
  }

  property("update, length") = forAll { (map: DeboxMap[K, V], k: K) =>
    val initialLength = map.length
    if (map.contains(k)) {
      map.update(k, 0)
      map.length == initialLength
    } else {
      map.update(k, 0)
      map.length == initialLength + 1
    }
  }

  property("getOrElse") = forAll { (map: DeboxMap[K, V], k: K, v: V) =>
    (map.contains(k) && (map.getOrElse(k, v) == map.get(k).get)) ||
      (!map.contains(k) && (map.getOrElse(k, v) == v))
  }

  property("getOrElseUpdate") = forAll { (map: DeboxMap[K, V], k: K, v: V) =>
    if (map.contains(k)) {
      val oldValue = map.get(k).get
      map.getOrElseUpdate(k, v) == oldValue
    } else {
      map.getOrElseUpdate(k, v) == v && map.contains(k) && map.get(k).get == v
    }
  }

  property("foreach") = forAll { (map: DeboxMap[K, V]) =>
    val entries = scala.collection.mutable.ListBuffer.empty[(K, V)]
    map.foreach { case (k, v) => entries.append((k, v)) }
    entries.length == map.length && entries.forall {
      case (k, v) =>
        map.get(k) == Some(v)
    }
  }

}
