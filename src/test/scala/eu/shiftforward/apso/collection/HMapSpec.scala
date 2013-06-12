package eu.shiftforward.apso.collection

import org.specs2.mutable.Specification

class HMapSpec extends Specification {

  "An HMap" should {

    val Key1 = new HMapKey[Int]
    val Key2 = new HMapKey[String]
    val Key3 = new HMapKey[List[Boolean]]
    val Key4 = new HMapKey[Int]

    "be correctly created from a list of key value pairs" in {
      val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      map.entries.toSet mustEqual Set(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))
    }

    "have working getters" in {
      val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      map(Key1) mustEqual 4
      map(Key4) must throwA[NoSuchElementException]

      map.get(Key1) must beSome(4)
      map.get(Key4) must beNone

      map.getOrElse(Key1, 0) mustEqual 4
      map.getOrElse(Key4, -1) mustEqual -1

      map.getOrElseUpdate(Key2, "") mustEqual "s"
      map.get(Key2) must beSome("s")
      map.getOrElseUpdate(Key4, -1) mustEqual -1
      map.get(Key4) must beSome(-1)
    }

    "have working setters" in {
      val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      map.put(Key4, 4)
      map.get(Key4) must beSome(4)

      map.put(Key1, 1)
      map.get(Key1) must beSome(1)

      map += (Key2 -> "2")
      map.get(Key2) must beSome("2")
    }

    "be copyable and mergeable" in {
      val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      val map2 = map.copy

      map mustEqual map2
      map.entries mustEqual map2.entries

      map2.put(Key1, 1)
      map.get(Key1) must beSome(4)

      val map3 = map ++ HMap(Key1 -> 1, Key4 -> 4)
      map3.entries.toSet mustEqual Set(Key1 -> 1, Key2 -> "s", Key3 -> List(false, true), Key4 -> 4)
    }

    "be mappeable" in {
      val map = HMap(Key1 -> 4, Key2 -> "s", Key3 -> List(false, true))

      map.map(_._1).toSet mustEqual Set(Key1, Key2, Key3)
      map.map(_._2).toSet mustEqual Set(4, "s", List(false, true))
    }
  }
}
