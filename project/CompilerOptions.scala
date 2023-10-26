import sbt.librarymanagement.CrossVersion

object CompilerOptions {

  private val base = Seq(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-language:higherKinds")

  private val opts211 = base ++ Seq(
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused"
  )

  private val opts212 = base ++ Seq(
    "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits",
    "-Xlint:-unused,_"
  )

  private val opts213 = base ++ Seq(
    "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits",
    "-Xsource:2.13"
  )

  private val opts300 = base // FIXME

  def apply(scalaVersion: String): Seq[String] = {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 11)) => opts211
      case Some((2, 12)) => opts212
      case Some((2, 13)) => opts213
      case Some((3, _))  => opts300
      case _             => Seq()
    }
  }
}