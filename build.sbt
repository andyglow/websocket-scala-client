import CustomGithubActions.*
import CustomGithubActions.aggregateCC
import CustomGithubActions.generateCC
import CustomGithubActions.uploadCC
import Dependencies.*
import ReleaseTransformations.*
import ScalaVersions.*
import java.net.URL
import sbt.*
import sbt.Defaults.*
import sbt.Keys.*
import sbt.librarymanagement.CrossVersion.PartialVersion
import xerial.sbt.Sonatype.*

ThisBuild / scalaVersion := scala211
//// https://github.com/xerial/sbt-sonatype/issues/71
//ThisBuild / publishTo  := sonatypePublishTo.value
ThisBuild / githubWorkflowTargetPaths           := Paths.Ignore(List("**.md"))
ThisBuild / githubWorkflowScalaVersions         := Seq(scala211, scala212, scala213, scala3)
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("11"), JavaSpec.temurin("17"))
ThisBuild / githubWorkflowBuildPostamble        := Seq(generateCC, aggregateCC, uploadCC)

def specificFolder(base: File, suffix: String): Seq[File] = Seq(base / "main" / s"scala-$suffix")

lazy val commons = Seq(
  organization     := "com.github.andyglow",
  homepage         := Some(new URL("http://github.com/andyglow/websocket-scala-client")),
  startYear        := Some(2019),
  organizationName := "andyglow",
  scalacOptions    := CompilerOptions(scalaVersion.value),
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  licenses               := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  sonatypeProfileName    := "com.github.andyglow",
  publishMavenStyle      := true,
  sonatypeProjectHosting := Some(GitHubHosting("andyglow", "websocket-scala-client", "andyglow@gmail.com")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/andyglow/websocket-scala-client"),
      "scm:git@github.com:andyglow/websocket-scala-client.git"
    )
  ),
  developers := List(
    Developer(
      id = "andyglow",
      name = "Andriy Onyshchuk",
      email = "andyglow@gmail.com",
      url = url("https://ua.linkedin.com/in/andyglow")
    )
  ),
  releaseCrossBuild             := true,
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
    pushChanges
  ),
  Compile / unmanagedSourceDirectories ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Nil
      case _             => specificFolder(sourceDirectory.value, "2.11+")
    }
  },
  Test / unmanagedSourceDirectories ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Nil
      case _             => specificFolder(sourceDirectory.value, "2.11+")
    }
  },
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest"    % "3.2.17" % Test,
    "org.mockito"    % "mockito-core" % "5.6.0"  % Test
  )
)

resolvers ++= Seq("snapshots", "releases").flatMap(Resolver.sonatypeOssRepos)

lazy val api = (projectMatrix in file("modules/api"))
  .dependsOn(simpleNettyEchoWebsocketServer % Test)
  .settings(
    commons,
    name := "websocket-api"
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val backendNetty = (projectMatrix in file("modules/backend-netty"))
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
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val backendAkka = (projectMatrix in file("modules/backend-akka"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-akka",
    libraryDependencies ++= Seq(
      akkaHttp(scalaVersion.value).cross(CrossVersion.binary),
      akkaStream(scalaVersion.value).cross(CrossVersion.binary)
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val backendPekko = (projectMatrix in file("modules/backend-pekko"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-pekko",
    libraryDependencies ++= Seq(
      pekkoHttp,
      pekkoStream
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions(_ == scala211))

lazy val backendJDK9 = (projectMatrix in file("modules/backend-jdk9"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-backend-jdk9",
    libraryDependencies ++= Seq(
      scalaStm,
      slf4jApi
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val serdeAvro4s = (projectMatrix in file("modules/serde-avro4s"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    commons,
    name := "websocket-serde-avro4s",
    libraryDependencies ++= Seq(
      avro4s(scalaVersion.value)
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions(_ == scala211))

// TODO: can be written entirely in the lowest scala version or in java so we don't need to rebuild it for other
//       scala versions as it only needed in tests
lazy val simpleNettyEchoWebsocketServer = (projectMatrix in file("modules/simple-netty-websocket-echo-server"))
  .settings(
    commons,
    name := "simple-netty-websocket-echo-server",
    libraryDependencies ++= Seq(
      nettyAll,
      nettyHttp,
      log4j2Api,
      log4j2Core,
      bcCore,
      bcPkix
    ),
    publish := false
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

//lazy val legacy = (projectMatrix in file("modules/_legacy"))
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
  .aggregate(
    api.projectRefs ++ backendNetty.projectRefs ++ backendJDK9.projectRefs ++ backendAkka.projectRefs ++ backendPekko.projectRefs: _*
  )
