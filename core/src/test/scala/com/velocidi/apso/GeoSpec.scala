package com.velocidi.apso

import org.specs2.mutable.Specification

class GeoSpec extends Specification {
  "A Geo object" should {
    "allow computing the distance between two points" in {
      // All of the asserted distances have been measured using Google Maps
      val p1 = (41.158131, -8.629235) // Boavista
      val p2 = (41.338871, -8.556241) // Trofa
      Geo.distance(p1, p2) must beCloseTo(21, 0.1)

      val p3 = (41.149967, -8.610243) // Porto
      val p4 = (40.416691, -3.700345) // Madrid
      Geo.distance(p3, p4) must beCloseTo(421, 0.3)

      val p5 = (40.714268, -74.005974) // New York
      val p6 = (48.856667, 2.350987) // Paris
      Geo.distance(p5, p6) must beCloseTo(5837, 0.1)

      val p7 = (59.913818, 10.738741) // Oslo
      val p8 = (-33.923775, 18.423346) // Cape Town
      Geo.distance(p7, p8) must beCloseTo(10458, 0.2)
    }

    "allow computing the distance between two points (curried version)" in {
      // All of the asserted distances have been measured using Google Maps
      val p1 = (41.158131, -8.629235) // Boavista
      val p2 = (41.338871, -8.556241) // Trofa
      Geo.distanceFrom(p1)(p2) must beCloseTo(21, 0.1)

      val p3 = (41.149967, -8.610243) // Porto
      val p4 = (40.416691, -3.700345) // Madrid
      Geo.distanceFrom(p3)(p4) must beCloseTo(421, 0.3)

      val p5 = (40.714268, -74.005974) // New York
      val p6 = (48.856667, 2.350987) // Paris
      Geo.distanceFrom(p5)(p6) must beCloseTo(5837, 0.1)

      val p7 = (59.913818, 10.738741) // Oslo
      val p8 = (-33.923775, 18.423346) // Cape Town
      Geo.distanceFrom(p7)(p8) must beCloseTo(10458, 0.2)
    }
  }
}
