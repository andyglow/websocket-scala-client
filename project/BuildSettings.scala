import sbt.Keys._
import sbt.{Defaults, Opts, _}

object BuildSettings {

  val projectId = "websocket-scala-client"
  val ver       = "0.3.0"

  lazy val coreSettings = Defaults.coreDefaultSettings ++ Seq(
    version := ver,
    organization := "com.github.andyglow",

    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8", "2.12.0"),

    scalacOptions in Compile        ++= Seq("-unchecked", "-deprecation", "-target:jvm-1.8", "-Ywarn-unused-import"),
    scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation", "-implicits", "-skip-packages", "samples"),
    scalacOptions in (Compile, doc) ++= Opts.doc.title("Websocket Scala Client"),
    scalacOptions in (Compile, doc) ++= Opts.doc.version(ver),

    libraryDependencies ++= Dependencies.all

  ) ++ Bintray.settings
}