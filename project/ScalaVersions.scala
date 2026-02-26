object ScalaVersions {
  val scala211 = "2.11.12"
  val scala212 = "2.12.21"
  val scala213 = "2.13.18"
  val scala3   = "3.8.2"

  def allScalaVersions(excluding: String => Boolean = _ => false): List[String] =
    List(scala211, scala212, scala213, scala3).filterNot(excluding)
}
