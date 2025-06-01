import java.net.URL
import xerial.sbt.Sonatype.GitHubHosting

ThisBuild / sonatypeProfileName    := "com.github.andyglow"
ThisBuild / licenses               := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("andyglow", "websocket-scala-client", "andyglow@gmail.com"))
ThisBuild / organization           := "com.github.andyglow"
ThisBuild / homepage               := Some(new URL("http://github.com/andyglow/websocket-scala-client"))
ThisBuild / startYear              := Some(2019)
ThisBuild / organizationName       := "andyglow"
ThisBuild / scmInfo                := Some(
  ScmInfo(
    url("https://github.com/andyglow/websocket-scala-client"),
    "scm:git@github.com:andyglow/websocket-scala-client.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "andyglow",
    name = "Andriy Onyshchuk",
    email = "andyglow@gmail.com",
    url = url("https://ua.linkedin.com/in/andyglow")
  )
)

resolvers ++= Seq("snapshots", "releases").flatMap(Resolver.sonatypeOssRepos)

// use `.withRank(KeyRanks.Invisible)` to mute Unnecessary Warning
(ThisBuild / publishMavenStyle).withRank(KeyRanks.Invisible) := true

// https://github.com/xerial/sbt-sonatype#buildsbt
// uses staging directory for release versions
// and publishes directly to sonatype servers for snapshots
// use `sonatypeBundleRelease` for releases
// and `sonatypeReleaseAll` for snapshots
ThisBuild / publishTo := sonatypePublishToBundle.value
