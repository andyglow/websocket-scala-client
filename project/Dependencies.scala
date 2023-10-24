import sbt.*

object Dependencies {
  lazy val Examples = config("example") extend Compile

  val slf4jVersion = "2.0.9"
  val nettyVersion = "4.1.100.Final"
  val scalaStmVersion = "0.11.1"
  val bcVersion = "1.76" // bouncycastle
  val pekkoHttpVersion = "1.0.0"
  val pekkoStreamVersion = "1.0.1"
  // log4j is only needed to test-we-server
  val log4jVersion = "2.21.0"

  val nettyAll    = "io.netty"                 % "netty-all"        % nettyVersion      % Compile
  val nettyHttp   = "io.netty"                 % "netty-codec-http" % nettyVersion      % Compile
  val slf4jApi    = "org.slf4j"                % "slf4j-api"        % slf4jVersion      % Compile
  val slf4jSimple = "org.slf4j"                % "slf4j-simple"     % slf4jVersion      % Examples
  val scalaStm    = "org.scala-stm"           %% "scala-stm"        % scalaStmVersion   % Compile
  val log4j2Api   = "org.apache.logging.log4j" % "log4j-api"        % log4jVersion      % Compile
  val log4j2Core  = "org.apache.logging.log4j" % "log4j-core"       % log4jVersion      % Compile
  val bcCore      = "org.bouncycastle"         % "bcprov-jdk18on"   % bcVersion         % Compile
  val bcPkix      = "org.bouncycastle"         % "bcpkix-jdk18on"   % bcVersion         % Compile
  val pekkoHttp   = "org.apache.pekko"        %% "pekko-http"       % pekkoHttpVersion  % Compile
  val pekkoStream = "org.apache.pekko"        %% "pekko-stream"     % pekkoStreamVersion% Compile


  object akkaHttp {
    def latestOssVersion = "10.2.10"
    def apply(scalaVer: String, oss: Boolean = false): ModuleID = {
      val v = ScalaVer.fromString(scalaVer) match {
        case Some(ScalaVer._211) => "10.1.9"
        case _ if oss            => latestOssVersion
        case _                   => "10.5.3"

      }

      "com.typesafe.akka" %% "akka-http" % v
    }
  }

  object akkaStream {
    def latestOssVersion = "2.5.32"
    def apply(scalaVer: String, oss: Boolean = false): ModuleID = {
      val v = ScalaVer.fromString(scalaVer) match {
        case Some(ScalaVer._211) => "2.5.32"
        case _ if oss            => latestOssVersion
        case _                   => "2.8.5"
      }

      "com.typesafe.akka" %% "akka-stream" % v
    }
  }
}
