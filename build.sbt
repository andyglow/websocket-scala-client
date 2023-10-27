import Dependencies.*
import ScalaVersions.*
import commandmatrix.Dimension
import sbt.*
import sbt.Keys.*
import sbt.internal.{ProjectMatrix, ProjectMatrixReference}

ThisBuild / scalaVersion := scala211

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"    % "3.2.17" % Test,
  "org.mockito"    % "mockito-core" % "5.6.0"  % Test
)

// format: off
/** ---------------------
  * Modules
  * -------
  */
// format: on

lazy val api = (projectMatrix in file("modules/api"))
  .dependsOn(simpleNettyEchoWebsocketServer % Test)
  .settings(
    name := "websocket-api"
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val backendNetty = (projectMatrix in file("modules/backend-netty"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
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
    name := "websocket-backend-akka",
    libraryDependencies ++= Seq(
      akkaHttp(scalaVersion.value).cross(CrossVersion.binary),
      akkaStream(scalaVersion.value).cross(CrossVersion.binary)
    ),
    includeScala211PlusFolders
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val backendPekko = (projectMatrix in file("modules/backend-pekko"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-pekko",
    libraryDependencies ++= Seq(
      pekkoHttp,
      pekkoStream
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions(_ == scala211))

lazy val backendJdkHttpClient = (projectMatrix in file("modules/backend-jdk-http-client"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-jdk-http-client",
    libraryDependencies ++= Seq(
      slf4jApi
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

lazy val serdeAvro4s = (projectMatrix in file("modules/serde-avro4s"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
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
    name := "simple-netty-websocket-echo-server",
    libraryDependencies ++= Seq(
      nettyAll,
      nettyHttp,
      log4j2Api,
      log4j2Core,
      bcCore,
      bcPkix
    ),
    disablePublishing
  )
  .jvmPlatform(scalaVersions = allScalaVersions())

// format: off
/** ---------------------
  * Root
  * ----
  */
// format: on

lazy val matrices = Seq(api, backendNetty, backendJdkHttpClient, backendAkka, backendPekko)
lazy val root = (project in file("."))
  .aggregate(
    matrices.flatMap(_.projectRefs): _*
  )
  .settings(
    name := "websocket-root"
  )
  .settings(disablePublishing)

// format: off
/** ---------------------
  * Custom Commands
  * ---------------
  */
// format: on

inThisBuild(
  Seq(
    // sbt-commandmatrix
    commands ++= CrossCommand.all(
      Seq("clean", "test"),
      matrices = matrices,
      dimensions = Seq(
        Dimension.scala("2.13", fullFor3 = true),
        Dimension.platform()
      )
    )
  )
)

// format: off
/** ---------------------
  * Utilities
  * ---------
  */
// format: on

def specificFolder(base: File, suffix: String): Seq[File] = Seq(base / "main" / s"scala-$suffix")

lazy val projectsToAggregate: String => List[ProjectMatrix] = {
  val projects = List(api, backendNetty, backendJdkHttpClient, backendAkka)
  scalaVersion => CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 11)) => projects :+ backendPekko
    case _ => projects
  }
}

val disablePublishing = Seq[Setting[_]](
  publishArtifact := false,
  publish / skip  := true
)

val includeScala211PlusFolders = Seq(
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
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
  }
)
