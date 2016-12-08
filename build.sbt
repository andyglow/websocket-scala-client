import sbt._
import sbt.Keys._
import sbt.Defaults._
import BuildSettings._
import Dependencies._

lazy val project = (Project(projectId, file("."))
  configs Examples
  settings inConfig(Examples)(compileBase ++ compileSettings ++ Seq(
    run     <<= Defaults.runTask(fullClasspath in Examples, mainClass in run, runner in run),
    runMain <<= Defaults.runMainTask(fullClasspath in Examples, runner in run)))
  settings coreSettings)