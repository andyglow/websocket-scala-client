import sbt.*

object Dependencies {
  lazy val Examples = config("example") extend Compile

  val slf4jVersion = "2.0.9"
  val nettyVersion = "4.1.100.Final"

  val nettyAll    = "io.netty"      % "netty-all"        % nettyVersion % Compile
  val nettyHttp   = "io.netty"      % "netty-codec-http" % nettyVersion % Compile
  val slf4jApi    = "org.slf4j"     % "slf4j-api"        % slf4jVersion % Compile
  val slf4jSimple = "org.slf4j"     % "slf4j-simple"     % slf4jVersion % Examples
  val scalaStm    = "org.scala-stm" %% "scala-stm"       % "0.11.1"     % Compile

  object akkaHttp {

    def apply(scalaVer: String): ModuleID = {
      val v = ScalaVer.fromString(scalaVer) match {
        case Some(ScalaVer._211) => "10.1.9"
        case _                   => "10.5.3"

      }

      "com.typesafe.akka" %% "akka-http" % v % Test
    }
  }

  object akkaStream {
    def apply(scalaVer: String): ModuleID = {
      val v = ScalaVer.fromString(scalaVer) match {
        case Some(ScalaVer._211) => "2.5.32"
        case _                   => "2.8.5"
      }

      "com.typesafe.akka" %% "akka-stream" % v % Test
    }
  }
}
