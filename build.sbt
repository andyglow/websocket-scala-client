import CustomGithubActions.{aggregateCC, generateCC, uploadCC}
import sbt.*
import sbt.Keys.*
import sbt.Defaults.*
import xerial.sbt.Sonatype.*
import ReleaseTransformations.*
import Dependencies.*
import ScalaVer.*
import CustomGithubActions.*
import sbt.librarymanagement.CrossVersion.PartialVersion

import java.net.URL


// https://github.com/xerial/sbt-sonatype/issues/71
ThisBuild / publishTo  := sonatypePublishTo.value
ThisBuild / githubWorkflowTargetPaths           := Paths.Ignore(List("**.md"))
ThisBuild / githubWorkflowScalaVersions         := ScalaVer.values.map(_.full)
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("11"))
ThisBuild / githubWorkflowBuildPostamble        := Seq(generateCC, aggregateCC, uploadCC)

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

    libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.2.17" % Test,
        "org.mockito" % "mockito-core" % "5.6.0" % Test
    )
)

resolvers ++= Seq("snapshots", "releases").flatMap(Resolver.sonatypeOssRepos)

lazy val excludeFrom211 = Seq(
  Compile / aggregate := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => false // exclude from 2.11 build as there is no 2.11 versions
      case _             => true
    }
  }
)

lazy val api = (project in file("modules/api"))
  .dependsOn(simpleNettyEchoWebsocketServer % Test)
  .settings(
    commons,
    name := "websocket-api"
  )

lazy val backendNetty = (project in file("modules/backend-netty"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-netty",
    libraryDependencies ++= Seq(
      nettyAll,
      nettyHttp,
      scalaStm,
      slf4jApi
    )
  )

lazy val backendAkka = (project in file("modules/backend-akka"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-akka",
    libraryDependencies ++= Seq(
      akkaHttp(scalaVersion.value).cross(CrossVersion.binary),
      akkaStream(scalaVersion.value).cross(CrossVersion.binary)
    )
  )

lazy val backendPekko = (project in file("modules/backend-pekko"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-pekko",
    libraryDependencies ++= Seq(
      pekkoHttp,
      pekkoStream
    ),
    excludeFrom211
  )

lazy val backendJDK9 = (project in file("modules/backend-jdk9"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-jdk9",
    libraryDependencies ++= Seq(
      scalaStm,
      slf4jApi
    )
  )

lazy val serdeAvro4s = (project in file("modules/serde-avro4s"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-serde-avro4s",
    libraryDependencies ++= Seq(
      avro4s(scalaVersion.value)
    ),
    excludeFrom211
  )

lazy val simpleNettyEchoWebsocketServer = (project in file("modules/simple-netty-websocket-echo-server"))
  .settings(
    commons,
    name := "simple-netty-websocket-echo-server",
    libraryDependencies ++= Seq(
      nettyAll,
      nettyHttp,
      log4j2Api,
      log4j2Core,
      bcCore, bcPkix
    ),
    publish := false
  )

//lazy val legacy = (project in file("modules/_legacy"))
//  .configs(Examples)
//  .settings(inConfig(Examples)(compileBase ++ compileSettings ++ Seq(
//    run     := Defaults.runTask(Examples / fullClasspath , run / mainClass, run / runner).evaluated,
//    runMain := Defaults.runMainTask(Examples / fullClasspath, run / runner).evaluated)))
//  .settings(
//      commons,
//      name := "websocket-scala-client",
//      libraryDependencies ++= Seq(
//          nettyAll,
//          nettyHttp,
//          scalaStm,
//          slf4jApi,
//          slf4jSimple,
//          akkaHttp(scalaVersion.value).cross(CrossVersion.binary),
//          akkaStream(scalaVersion.value).cross(CrossVersion.binary)))

lazy val root = (project in file("."))
  .aggregate(api, backendNetty, backendJDK9, backendAkka, backendPekko)
