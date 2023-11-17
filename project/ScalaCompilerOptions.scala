import sbt.librarymanagement.CrossVersion

object ScalaCompilerOptions {

  // inliner must be run with "clean; compile", it's not incremental
  // https://www.lightbend.com/blog/scala-inliner-optimizer
  // https://docs.scala-lang.org/overviews/compiler-options/index.html
  private val optimizeInline = Seq(
    "-opt:l:inline",
    "-opt-inline-from:com.github.andyglow.**",
    "-opt-warnings:none"
    // "-opt-warnings:any-inline-failed"
  )

  private val base = Seq(
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions"
  )

  private val opts211 = base ++ Seq(
    "-language:existentials",
    "-language:postfixOps",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Yrangepos"
  ) ++ Seq(
    "-Xsource:2.11",
    "-Yno-adapted-args",
    "-Xfatal-warnings"
  )

  private val opts212 = base ++ Seq(
    "-language:existentials",
    "-language:postfixOps",
    "-Xlint:-unused,_",
    "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits",
    "-Ywarn-dead-code",
    "-Yrangepos"
  ) ++ Seq(
    "-Xsource:2.12",
    "-Yno-adapted-args"
  ) ++ optimizeInline

  private val opts213 = base ++ Seq(
    "-language:existentials",
    "-language:postfixOps",
    "-Xlint",
    "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits",
    "-Ywarn-dead-code",
    "-Yrangepos"
  ) ++ Seq(
    "-Xsource:2.13",
    "-Xfatal-warnings",
    "-Wconf:any:warning-verbose",
    "-release",
    "8"
  ) ++ optimizeInline

  private val opts300 = Seq(
    "-language:higherKinds",
    "-language:implicitConversions",
    "-explain",
    "-release",
    "8"
  )

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
