credentials ++= {
  val nexusUser = System.getenv("SONATYPE_USERNAME")
  val nexusPass = System.getenv("SONATYPE_PASSWORD")

  if (nexusUser == null || nexusPass == null) Nil
  else Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", nexusUser, nexusPass))
}
