object ScalaVersions {
  val scala211 = "2.11.12"
  val scala212 = "2.12.19"
  val scala213 = "2.13.13"
  val scala3   = "3.4.1"

  def allScalaVersions(excluding: String => Boolean = _ => false): List[String] =
    List(scala211, scala212, scala213, scala3).filterNot(excluding)
}
