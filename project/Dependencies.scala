import sbt._

object Dependencies {
  lazy val Examples = config("example") extend Compile

  val slf4jVersion = "2.0.1"
  val nettyVersion = "4.1.84.Final"

  val nettyAll    = "io.netty"      % "netty-all"        % nettyVersion % Compile
  val nettyHttp   = "io.netty"      % "netty-codec-http" % nettyVersion % Compile
  val slf4jApi    = "org.slf4j"     % "slf4j-api"        % slf4jVersion % Compile
  val slf4jSimple = "org.slf4j"     % "slf4j-simple"     % slf4jVersion % Examples
  val scalaStm    = "org.scala-stm" %% "scala-stm"       % "0.11.1"     % Compile

  object akkaHttp {

    def apply(scalaVer: ScalaVer) = {
      val v = scalaVer match {
        case ScalaVer._211 => "10.1.9"
        case _             => "10.2.6"

      }

      "com.typesafe.akka" %% "akka-http" % v % Test
    }
  }

  object akkaStream {
    def apply(scalaVer: ScalaVer) = {
      val v = scalaVer match {
        case ScalaVer._211 => "2.5.32"
        case _             => "2.6.16"
      }

      "com.typesafe.akka" %% "akka-stream" % v % Test
    }
  }
}
