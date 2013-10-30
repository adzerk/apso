package eu.shiftforward.apso

import org.specs2.mutable._
import eu.shiftforward.apso.Implicits._

class ImplicitsSpec extends Specification {

  "A PimpedListMap" should {
    "convert correctly a list of maps into a map of lists" in {
      List[Map[Int, Int]]().sequenceOnMap() === Map[Int, List[Int]]()

      List(Map(1 -> 2), Map(1 -> 3), Map(2 -> 3)).sequenceOnMap() ===
        Map(1 -> List(2, 3), 2 -> List(3))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a")).sequenceOnMap() ===
        Map(1 -> List("c"), 2 -> List("b"), 3 -> List("a"))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a"), Map(1 -> "c2", 4 -> "aa")).sequenceOnMap() ===
        Map(1 -> List("c", "c2"), 2 -> List("b"), 3 -> List("a"), 4 -> List("aa"))
    }

    "convert correctly a list of maps into a map of lists with a zero value" in {
      List[Map[Int, Int]]().sequenceOnMap(Some(0)) === Map[Int, List[Int]]()

      List(Map(1 -> 2), Map(1 -> 3), Map(2 -> 3)).sequenceOnMap(Some(0)) ===
        Map(1 -> List(2, 3, 0), 2 -> List(0, 0, 3))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a")).sequenceOnMap(Some("")) ===
        Map(1 -> List("c"), 2 -> List("b"), 3 -> List("a"))

      List(Map(1 -> "c", 2 -> "b", 3 -> "a"), Map(1 -> "c2", 4 -> "aa")).sequenceOnMap(Some("")) ===
        Map(1 -> List("c", "c2"), 2 -> List("b", ""), 3 -> List("a", ""), 4 -> List("", "aa"))
    }
  }
}
