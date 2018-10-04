package com.velocidi.apso

import scala.math.{ abs, acos, cos, sin, toRadians }

/**
 * Object containing geo-location functions.
 */
object Geo {
  /**
   * A pair of `Double` representing, respectively, the latitude and longitude of a position.
   */
  type Coordinates = (Double, Double)

  private[this] val EARTH_RADIUS = 6371.009 // kilometers

  /**
   * Returns the distance in kilometers between two points on the planet Earth, calculated by the
   * spherical law of cosines (https://en.wikipedia.org/wiki/Great-circle_distance#Formulas).
   * @param p1 the coordinates of the first point
   * @param p2 the coordinates of the second point
   * @return the distance in kilometers between two points on the planet Earth.
   */
  def distance(p1: Coordinates, p2: Coordinates) =
    distanceFrom(p1)(p2)

  /**
   * Returns a function that measures the distance to the point `p1` on the planet Earth, calculated
   * by the spherical law of cosines (https://en.wikipedia.org/wiki/Great-circle_distance#Formulas).
   * @param p1 the coordinates of the first point
   * @return a function that given a point `p2` returns the distance in kilometers to the point `p1`.
   */
  def distanceFrom(p1: Coordinates): Coordinates => Double = {
    val lat1 = toRadians(p1._1)
    val sinLat1 = sin(lat1)
    val cosLat1 = cos(lat1)

    p2 => {
      val deltaLong = toRadians(abs(p1._2 - p2._2))
      val lat2 = toRadians(p2._1)

      val c = acos((sinLat1 * sin(lat2)) + (cosLat1 * cos(lat2) * cos(deltaLong)))

      EARTH_RADIUS * c
    }
  }
}
