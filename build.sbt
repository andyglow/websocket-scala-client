import sbt._
import sbt.Keys._
import sbt.Defaults._
import xerial.sbt.Sonatype._
import ReleaseTransformations._
import Dependencies._

// https://github.com/xerial/sbt-sonatype/issues/71
publishTo in ThisBuild := sonatypePublishTo.value

lazy val commons = Seq(

    organization := "com.github.andyglow",

    homepage := Some(new URL("http://github.com/andyglow/websocket-scala-client")),

    startYear := Some(2019),

    organizationName := "andyglow",

    scalaVersion := "2.13.0",

    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),

    scalacOptions ++= {
        val options = Seq(
            "-encoding", "UTF-8",
            "-feature",
            "-unchecked",
            "-deprecation",
            "-Xfatal-warnings",
            "-Xlint",
            "-Yno-adapted-args",
            "-Ywarn-dead-code",
            "-Ywarn-numeric-widen",
            "-Xfuture",
            "-language:experimental.macros")

        // WORKAROUND https://github.com/scala/scala/pull/5402
        CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, 12)) => options.map {
                case "-Xlint"               => "-Xlint:-unused,_"
                case "-Ywarn-unused-import" => "-Ywarn-unused:imports,-patvars,-privates,-locals,-params,-implicits"
                case other                  => other
            }
            case Some((2, n)) if n >= 13  => options.filterNot { opt =>
                opt == "-Yno-adapted-args" || opt == "-Xfuture"
            } :+ "-Xsource:2.13"
            case _             => options
        }
    },

    scalacOptions in (Compile,doc) ++= Seq(
        "-groups",
        "-implicits",
        "-no-link-warnings"),

    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),

    sonatypeProfileName := "com.github.andyglow",

    publishMavenStyle := true,

    sonatypeProjectHosting := Some(
        GitHubHosting(
            "andyglow",
            "websocket-scala-client",
            "andyglow@gmail.com")),

    scmInfo := Some(
        ScmInfo(
            url("https://github.com/andyglow/websocket-scala-client"),
            "scm:git@github.com:andyglow/websocket-scala-client.git")),

    developers := List(
        Developer(
            id    = "andyglow",
            name  = "Andriy Onyshchuk",
            email = "andyglow@gmail.com",
            url   = url("https://ua.linkedin.com/in/andyglow"))),

    releaseCrossBuild := true,

    releasePublishArtifactsAction := PgpKeys.publishSigned.value,

    releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
        setNextVersion,
        commitNextVersion,
        ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
        pushChanges),

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

lazy val root = (project in file("."))
  .configs(Examples)
  .settings(inConfig(Examples)(compileBase ++ compileSettings ++ Seq(
    run     := Defaults.runTask(fullClasspath in Examples, mainClass in run, runner in run).evaluated,
    runMain := Defaults.runMainTask(fullClasspath in Examples, runner in run).evaluated)))
  .settings(
      commons,
      name := "websocket-scala-client",
      libraryDependencies ++= all)

