import CustomGithubActions.{aggregateCC, generateCC, uploadCC}
import ScalaVersions.{scala211, scala212, scala213, scala3}

ThisBuild / githubWorkflowTargetPaths           := Paths.Ignore(List("**.md"))
ThisBuild / githubWorkflowScalaVersions         := Seq(scala211, scala212, scala213, scala3)
ThisBuild / githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("11"), JavaSpec.temurin("17"))
ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / githubWorkflowBuildPostamble        := Seq(generateCC, aggregateCC, uploadCC)
ThisBuild / scalacOptions          := CompilerOptions(scalaVersion.value)
