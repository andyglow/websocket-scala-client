import bintray.BintrayKeys._
import sbt.Keys._
import sbt._
import BuildSettings._

object Bintray {

  lazy val settings = Seq(
    publishArtifact in Test := false,
    licenses += ("GPL-3.0", url("https://www.gnu.org/licenses/gpl-3.0.html")),
    bintrayPackageLabels := Seq("scala", "tools", "websocket", "client"),
    bintrayRepository := "scala-tools",
    homepage := Some(url(s"http://github.com/andyglow/$projectId")),
    checksums := Seq(),
    pomExtra :=
      <scm>
        <url>git://github.com/andyglow/${projectId}.git</url>
        <connection>scm:git://github.com/andyglow/${projectId}.git</connection>
      </scm>
        <developers>
          <developer>
            <id>andyglow</id>
            <name>Andrey Onistchuk</name>
            <url>https://ua.linkedin.com/in/andyglow</url>
          </developer>
        </developers>
  )

}