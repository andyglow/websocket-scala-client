import sbt._
import sbt.Keys._
import sbt.Defaults._
import xerial.sbt.Sonatype._
import ReleaseTransformations._
import Dependencies._
import ScalaVer._


// https://github.com/xerial/sbt-sonatype/issues/71
ThisBuild / publishTo  := sonatypePublishTo.value

lazy val commons = ScalaVer.settings ++ Seq(

    organization := "com.github.andyglow",

    homepage := Some(new URL("http://github.com/andyglow/websocket-scala-client")),

    startYear := Some(2019),

    organizationName := "andyglow",

    scalacOptions := CompilerOptions(scalaV.value),

    Compile / doc / scalacOptions ++= Seq(
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

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

lazy val root = (project in file("."))
  .configs(Examples)
  .settings(inConfig(Examples)(compileBase ++ compileSettings ++ Seq(
    run     := Defaults.runTask(Examples / fullClasspath , run / mainClass, run / runner).evaluated,
    runMain := Defaults.runMainTask(Examples / fullClasspath, run / runner).evaluated)))
  .settings(
      commons,
      name := "websocket-scala-client",
      libraryDependencies ++= Seq(
          nettyAll,
          nettyHttp,
          scalaStm,
          slf4jApi,
          slf4jSimple,
          akkaHttp(scalaV.value).cross(CrossVersion.for3Use2_13),
          akkaStream(scalaV.value).cross(CrossVersion.for3Use2_13)))

