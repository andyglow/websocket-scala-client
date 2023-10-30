import Dependencies.*
import ScalaVersions.*
import IntellijIdeaScalaVersion.*
import commandmatrix.Dimension
import sbt.*
import sbt.Keys.*
import sbt.internal.ProjectMatrix


ThisBuild / scalaVersion := scala213

// {{{ Configure Intellij Idea
// Scala Version in IDE
val intellijIdeaScalaVersion = IntellijIdeaScalaVersion(scala213)
// ???
Global / excludeLintKeys += ideSkipProject
// }}}

/**
  * Early Semver
  *
  * Given a version number major.minor.patch, you MUST increment the:
  * - [[major]] version if backward [[binary]] compatibility is broken,
  * - [[minor]] version if backward [[source]] compatibility is broken, and
  * - [[patch]] version to signal neither [[binary]] nor [[source]] incompatibility.
  * When the [[major]] version is [[0]], a [[minor]] version increment *MAY* contain
  * both [[source]] and [[binary]] breakages, but a [[patch]] version increment *MUST* remain [[binary]] compatible.
  *
  * Links
  * - https://www.scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html
  * - https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#versioning-scheme---communicating-compatibility-breakages
  */
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / libraryDependencies ++= Seq(
  scalatest,
  mockito,
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
    name := "websocket-api",
    commonOptions,
    libraryDependencies += scalatestplus(scalaVersion.value),
  )
  .jvmOnly(intellijIdeaScalaVersion)


lazy val backendNetty = (projectMatrix in file("modules/backend-netty"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-netty",
    commonOptions,
    libraryDependencies ++= Seq(
      nettyAll,
      nettyHttp,
      scalaStm,
      slf4jApi
    )
  )
  .jvmOnly(intellijIdeaScalaVersion)

lazy val backendAkka = (projectMatrix in file("modules/backend-akka"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-akka",
    commonOptions,
    libraryDependencies ++= Seq(
      akkaHttp(scalaVersion.value).cross(CrossVersion.binary),
      akkaStream(scalaVersion.value).cross(CrossVersion.binary)
    ),
    includeScala211PlusFolders
  )
  .jvmOnly(intellijIdeaScalaVersion)

lazy val backendPekko = (projectMatrix in file("modules/backend-pekko"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-pekko",
    commonOptions,
    libraryDependencies ++= Seq(
      pekkoHttp,
      pekkoStream
    )
  )
  .jvmOnly(intellijIdeaScalaVersion, excluding = { case `scala211` => })


lazy val backendJdkHttpClient = (projectMatrix in file("modules/backend-jdk-http-client"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-backend-jdk-http-client",
    commonOptions,
    libraryDependencies ++= Seq(
      slf4jApi
    )
  )
  .jvmOnly(intellijIdeaScalaVersion)

lazy val serdeAvro4s = (projectMatrix in file("modules/serde-avro4s"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-serde-avro4s",
    commonOptions,
    libraryDependencies ++= Seq(
      avro4s(scalaVersion.value)
    )
  )
  .jvmOnly(intellijIdeaScalaVersion, excluding = { case `scala211` | `scala3` => })

lazy val serdeJsoniterScala = (projectMatrix in file("modules/serde-jsoniter"))
  .dependsOn(api % "test->test;compile->compile")
  .settings(
    name := "websocket-serde-avro4s",
    commonOptions,
    libraryDependencies ++= Seq(
      jsoniterScala(scalaVersion.value).core % Compile,
      jsoniterScala(scalaVersion.value).macros % Test,
    )
  )
  .jvmOnly(intellijIdeaScalaVersion)

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

lazy val matrices = Seq(
  api,
  backendNetty,
  backendJdkHttpClient,
  backendAkka,
  backendPekko,
  serdeAvro4s,
  serdeJsoniterScala
)
lazy val root = (project in file("."))
  .aggregate(
    matrices.flatMap(_.projectRefs): _*
  )
  .settings(
    name := "websocket-root",
    disablePublishing,
  )
  .settings()

// format: off
/** ---------------------
  * Custom Matrix Commands
  * ---------------
  */
// format: on

inThisBuild(
  Seq(
    // sbt-commandmatrix
    commands ++= CrossCommand.all(
      Seq(
        "clean",
        "test",
        "publishSigned"
      ),
      matrices = matrices,
      dimensions = Seq(
        Dimension.scala("2.13", fullFor3 = false),
        Dimension.platform()
      )
    )
  )
)
val scalaVersionDimension = Seq("2_11", "2_12", "2_13", "3_3")
addCommandAlias("publishMatrix", scalaVersionDimension.map(v => s";publishSigned-$v-jvm").mkString)

// format: off
/** ---------------------
  * Utilities
  * ---------
  */
// format: on

def specificFolder(base: File, suffix: String): Seq[File] = Seq(base / "main" / s"scala-$suffix")

lazy val projectsToAggregate: String => List[ProjectMatrix] = {
  val projects = List(api, backendNetty, backendJdkHttpClient, backendAkka)
  scalaVersion =>
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 11)) => projects :+ backendPekko
      case _             => projects
    }
}

val disablePublishing = Seq[Setting[_]](
  publishArtifact := false,
  publish / skip  := true
)

val includeScala211PlusFolders = Seq[Setting[_]](
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
)

lazy val commonOptions = Seq[Setting[_]](
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  scalacOptions := ScalaCompilerOptions(scalaVersion.value),
)
