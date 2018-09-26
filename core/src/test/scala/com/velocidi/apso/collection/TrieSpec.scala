package com.velocidi.apso.collection

import org.specs2.mutable._
import org.specs2.matcher.MatchResult

class TrieSpec extends Specification {
  "A Trie" should {
    def checkTree[A, B](input: Seq[A], v: B, trie: Trie[A, B]): MatchResult[_] = {
      input match {
        case Seq(h, t @ _*) =>
          trie.nodes.get(h) must beLike {
            case Some(trie) =>
              checkTree(t, v, trie)
          }

        case _ =>
          trie.value === Some(v)
      }
    }

    "support sets" in {
      val t = Trie[Char, Int]()
      val t1 = t.set("hello", 1)
      checkTree("hello", 1, t1)
      val t2 = t1.set("hell", 2)
      checkTree("hello", 1, t2)
      checkTree("hell", 2, t2)
      val t3 = t2.set("hello", 3)
      checkTree("hello", 3, t3)
      checkTree("hell", 2, t3)
    }

    "support gets" in {
      val t = Trie[Char, Int]()
        .set("hello", 1)
        .set("hell", 2)
        .set("hellos", 3)

      t.get("hello") === Some(1)
      t.get("hell") === Some(2)
      t.get("hellos") === Some(3)
    }
  }
}
