import sbt.*

object Dependencies {
  lazy val Examples = config("example") extend Compile

  val slf4jVersion       = "2.0.16"
  val nettyVersion       = "4.1.115.Final"
  val scalaStmVersion    = "0.11.1"
  val bcVersion          = "1.76" // bouncycastle
  val pekkoHttpVersion   = "1.1.0"
  val pekkoStreamVersion = "1.1.2"
  // log4j is only needed to test-we-server
  val log4jVersion = "2.21.0"

  val scalatestVersion = "3.2.19"
  val mockitoVersion   = "5.14.2"

  val nettyAll    = "io.netty"                 % "netty-all"        % nettyVersion       % Compile
  val nettyHttp   = "io.netty"                 % "netty-codec-http" % nettyVersion       % Compile
  val slf4jApi    = "org.slf4j"                % "slf4j-api"        % slf4jVersion       % Compile
  val slf4jSimple = "org.slf4j"                % "slf4j-simple"     % slf4jVersion       % Examples
  val scalaStm    = "org.scala-stm"           %% "scala-stm"        % scalaStmVersion    % Compile
  val log4j2Api   = "org.apache.logging.log4j" % "log4j-api"        % log4jVersion       % Compile
  val log4j2Core  = "org.apache.logging.log4j" % "log4j-core"       % log4jVersion       % Compile
  val bcCore      = "org.bouncycastle"         % "bcprov-jdk18on"   % bcVersion          % Compile
  val bcPkix      = "org.bouncycastle"         % "bcpkix-jdk18on"   % bcVersion          % Compile
  val pekkoHttp   = "org.apache.pekko"        %% "pekko-http"       % pekkoHttpVersion   % Compile
  val pekkoStream = "org.apache.pekko"        %% "pekko-stream"     % pekkoStreamVersion % Compile

  val scalatest = "org.scalatest" %% "scalatest"    % scalatestVersion % Test
  val mockito   = "org.mockito"    % "mockito-core" % mockitoVersion   % Test

  object scalatestplus {

    def apply(scalaVersion: String): ModuleID = {
      val (a, v) = CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 11)) => ("scalacheck-1-15", "3.2.3.0")
        case _             => ("scalacheck-1-17", "3.2.18.0")
      }

      "org.scalatestplus" %% a % v
    }
  }

  object avro4s {

    def apply(scalaVersion: String): ModuleID = {
      val v = CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, _)) => "4.1.1"
        case _            => "5.0.5" // for scala 3
      }

      "com.sksamuel.avro4s" %% "avro4s-core" % v
    }
  }

  object jsoniterScala {
    trait jsoniterScala {
      def core: ModuleID
      def macros: ModuleID
    }

    def apply(scalaVersion: String): jsoniterScala = {
      val v = CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 11)) => "2.13.3.2"
        case _             => "2.24.4"
      }

      new jsoniterScala {
        override val core   = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % v
        override val macros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % v
      }
    }
  }

  object akkaHttp {
    def apply(scalaVersion: String, oss: Boolean = false): ModuleID = {
      val v = CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 11)) => "10.1.9"
        case _             => "10.5.3"

      }

      "com.typesafe.akka" %% "akka-http" % v
    }
  }

  object akkaStream {
    def apply(scalaVersion: String, oss: Boolean = false): ModuleID = {
      val v = CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 11)) => "2.5.32"
        case _             => "2.8.5"
      }

      "com.typesafe.akka" %% "akka-stream" % v
    }
  }
}
