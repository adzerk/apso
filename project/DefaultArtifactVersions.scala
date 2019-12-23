package sbt

import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

object DefaultArtifactVersions extends DefaultArtifactVersions

/**
 * Trait providing a constant for defining default versions in library
 * dependencies. It allows one to define dependencies such as the following,
 * reusing the constant in different subprojects:
 *
 * {{{
 * "com.typesafe.akka" % "akka-http" % defaultVersion
 * }}}
 *
 * The default versions are obtained from the `defaultModuleVersions` and
 * `defaultOrganizationVersions` maps, in this order. That is, if a module
 * matches one with default version and there is a default version specified
 * both for the module and for the organization, the one defined for the module
 * is preferred. In case there is no default for the module and
 * `AdStaxDefaultVersion` is used, the module's
 * [[http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html
 * `latest.integration`]] version is used.
 */
trait DefaultArtifactVersions {
  /**
   * Default versions for organizations.
   */
  final val defaultOrganizationVersions = Map(
    "com.amazonaws"          -> "1.11.553",
    "com.sksamuel.elastic4s" -> "7.1.2",
    "com.typesafe.akka"      -> "2.6.0",
    "io.circe"               -> "0.12.3",
    "io.netty"               -> "4.1.43.Final",
    "org.specs2"             -> "4.8.1")


  /**
   * Default versions for modules.
   */
  final val defaultModuleVersions = Map(
    ("com.github.nscala-time", "nscala-time")         -> "2.22.0",
    ("com.typesafe",           "config")              -> "1.4.0",
    ("com.typesafe.akka",      "akka-http")           -> "10.1.11",
    ("com.typesafe.akka",      "akka-http-testkit")   -> "10.1.11",
    ("net.ruippeixotog",       "akka-testkit-specs2") -> "0.2.3",
    ("org.scalacheck",         "scalacheck")          -> "1.14.2")

  /**
   * The value to be used in the library dependency in order to fetch the
   * default version.
   */
  object defaultVersion

  /**
   * Class that pimps `OrganizationArtifactName` with a method to generate a `ModuleID`
   * using `%` with `AdStaxDefaultVersion`.
   *
   * @param artifactID the `OrganizationArtifactName` to be pimped
   */
  implicit class DefaultArtifactID(artifactID: OrganizationArtifactName) {
    /**
     * Returns a `ModuleID` with a default version if used with
     * `AdStaxDefaultVersion`.
     *
     * @param default the value that signals that a default version should be
     *                used. It is a singleton type, so there's only a single
     *                instance that this method accepts
     * @return a `ModuleID` with the default specified version, or with
     *         `latest.integration` if no default version is specified.
     */
    def %(default: defaultVersion.type): ModuleID = {
      val tempModule = artifactID % "latest.integration"
      val organization = tempModule.organization
      val name = tempModule.name

      val defaultVersion = defaultModuleVersions.get(organization, name) orElse
        defaultOrganizationVersions.get(organization)
      defaultVersion match {
        case Some(v) => artifactID % v
        case None => throw new Exception(s"Could not get default version for $organization % $name")
      }
    }
  }
}
