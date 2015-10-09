package eu.shiftforward.apso

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
  def distance(p1: Coordinates, p2: Coordinates) = {
    val deltaLong = toRadians(abs(p1._2 - p2._2))
    val lat1 = toRadians(p1._1)
    val lat2 = toRadians(p2._1)

    val c = acos((sin(lat1) * sin(lat2)) + (cos(lat1) * cos(lat2) * cos(deltaLong)))

    EARTH_RADIUS * c
  }
}
