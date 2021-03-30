import sbt._

object Dependencies {
  lazy val Examples = config("example") extend Compile

  val nettyVersion = "4.1.62.Final"
  val slf4jVersion = "1.7.30"

  val nettyAll    = "io.netty"      % "netty-all" 			  % nettyVersion  % Compile
  val nettyHttp   = "io.netty"      % "netty-codec-http" 	% nettyVersion  % Compile
  val slf4jApi    = "org.slf4j"     % "slf4j-api"         % slf4jVersion  % Compile
  val slf4jSimple = "org.slf4j"     % "slf4j-simple"      % slf4jVersion  % Examples
  val scalaStm    = "org.scala-stm" %% "scala-stm"        % "0.11.0"       % Compile

  val akkaHttp    = "com.typesafe.akka" %% "akka-http"    % "10.1.13"     % Test
  val akkaStream  = "com.typesafe.akka" %% "akka-stream"  % "2.5.32"      % Test

  val all = Seq(nettyAll, nettyHttp, scalaStm, slf4jApi, slf4jSimple, akkaHttp, akkaStream)
}