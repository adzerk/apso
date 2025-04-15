package com.kevel.apso.circe

import scala.annotation.tailrec
import scala.util.Try

import io.circe._

/** Object containing implicit classes and methods related to JSON handling.
  */
object Implicits {

  object ToInt {
    def unapply(str: String) = Try(str.toInt).toOption
  }

  private def flattenedKeyValueSetAux(json: Json, separator: String = "."): Vector[(String, Json)] = {
    json.asObject match {
      case None => Vector.empty
      case Some(jo) =>
        val builder = Vector.newBuilder[(String, Json)]
        jo.toMap.foreach {
          case (k, v) if v.isObject =>
            flattenedKeyValueSetAux(v, separator).foreach { case (kk, vv) =>
              val path = new StringBuilder(k.length + separator.length + kk.length)
                .append(k)
                .append(separator)
                .append(kk)
                .toString
              builder += ((path, vv))
            }
          case (k, v) =>
            builder += ((k, v))
        }
        builder.result()
    }
  }

  final implicit class ApsoJsonObject(val json: Json) extends AnyVal {

    /** Returns a set of keys-value pairs of this object where nested keys are separated by a separator character.
      *
      * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".") = Set("a" -> JNumber(1),"b.c" -> JNumber(2), "d" -> JNull)
      *
      * @param separator
      *   character separator to use
      * @return
      *   flattened key set
      */
    def flattenedKeyValueSet(separator: String = "."): Set[(String, Json)] = {
      flattenedKeyValueSetAux(json, separator).toSet
    }

    /** Returns a set of keys of this object where nested keys are separated by a separator character.
      *
      * Eg. {"a":1,"b":{"c":2},"d":null}.flattenedKeySet(".", ignoreNull = true) = Set("a","b.c")
      *
      * @param separator
      *   character separator to use
      * @param ignoreNull
      *   if set, fields with a null value are ignored
      * @return
      *   flattened key set
      */
    def flattenedKeySet(separator: String = ".", ignoreNull: Boolean = true): Set[String] =
      flattenedKeyValueSetAux(json, separator)
        .filter { case (_, v) =>
          !ignoreNull || !v.isNull
        }
        .map(_._1)
        .toSet

    /** Returns the value of the field on the end of the tree, separated by the separator character.
      *
      * Eg. {"a":{"b":1}}.getField("a.b") = 1
      *
      * @param fieldPath
      *   path from the root of the json object to the field
      * @param separator
      *   character that separates each element of the path
      * @tparam A
      *   type of the field value
      * @return
      *   an option with the field value
      */
    def getField[A: Decoder](fieldPath: String, separator: Char = '.'): Option[A] =
      getCursor(fieldPath, separator).as[A].fold(_ => None, Some(_))

    /** Deletes a field on a json object.
      *
      * Eg. {"a":1,"b":{"c":2},"d":null}.deleteField("b.c") = {"a":1,"b":{},"d":null}
      *
      * @param fieldPath
      *   path from the root of the json object to the field
      * @param separator
      *   character that separates each element of the path
      * @return
      *   the json without the deleted value
      */
    def deleteField(fieldPath: String, separator: Char = '.'): Json = {
      require(fieldPath.nonEmpty, "The field path must have value.")

      getCursor(fieldPath, separator).delete.top.getOrElse(json)
    }

    /** Returns a cursor on the field on the end of the tree, separated by the separator character.
      *
      * @param fieldPath
      *   path from the root of the json object to the field
      * @param separator
      *   character that separates each element of the path
      * @return
      *   cursor to the field value
      */
    def getCursor(fieldPath: String, separator: Char): ACursor =
      fieldPath
        .split(separator)
        .foldLeft(json.hcursor: ACursor) { case (cursor, field) =>
          cursor.downField(field)
        }
  }

  /** Creates a Json from a sequence of pairs of dot-separated (or other separator) paths with the corresponding leaf
    * values (eg. `List(("root.leaf1", "leafVal1"), ("root.leaf2", "leafVal2"))`
    * @param paths
    *   the sequence of dot-separated (or other separator) paths
    * @param separatorRegex
    *   regex to use to separate fields
    * @return
    *   the resulting Json object
    */
  def fromFullPaths(paths: Seq[(String, Json)], separatorRegex: String = "\\."): Json = {
    def createJson(keys: Seq[String], value: Json): Json = {
      keys match {
        case Nil    => value
        case h :: t => Json.obj(h -> createJson(t, value))
      }
    }

    @tailrec
    def fromFullPathsRec(paths: Seq[(String, Json)], acc: Json): Json = {
      paths match {
        case Nil => acc
        case (path, value) :: rem =>
          val newAcc = acc.deepMerge(createJson(path.split(separatorRegex).toList, value))
          fromFullPathsRec(rem, newAcc)
      }
    }

    fromFullPathsRec(paths, Json.obj())
  }

  final implicit class ApsoJsonEncoder[A](val encoder: Encoder[A]) extends AnyVal {

    /** Returns an encoder that removes the json fields that are null, applied on the root of the json.
      * @return
      *   encoder that filters null fields.
      */
    def withoutNulls: Encoder[A] = encoder.mapJson(_.mapObject(_.filter(!_._2.isNull)))

    /** Returns an encoder that adds an element to the root of the json object.
      *
      * @param key
      *   the key of the element
      * @param value
      *   the value of the element
      * @return
      *   encoder that adds element to the json
      */
    def withExtraField(key: String, value: Json): Encoder[A] = encoder.mapJson(_.mapObject(_.add(key, value)))
  }
}
