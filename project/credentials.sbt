val nexusUser = System.getenv("SF_NEXUS_USERNAME")
val nexusPass = System.getenv("SF_NEXUS_PASSWORD")

credentials ++= {
  if (nexusUser == null || nexusPass == null) Nil
  else Seq(
    Credentials(
      "Nexus Repository Manager",
      "NEXUS_URL",
      nexusUser,
      nexusPass))
}
