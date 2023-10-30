import ScalaVersions.allScalaVersions
import commandmatrix.extra.{MatrixAction, ProjectMatrixExtraOps}
import sbt.Keys.scalaVersion
import sbt.VirtualAxis
import sbt.internal.ProjectMatrix
import sbtide.Keys.ideSkipProject

case class IntellijIdeaScalaVersion(version: String) {
  // Axis Filter to export only certain scala version originated projectRef to IDE
  val oneAxisAtATimeExportedTpIntellijIdea = MatrixAction
    .ForPlatforms(VirtualAxis.jvm)
    .Configure(_.settings(ideSkipProject := (scalaVersion.value != version)))
}

object IntellijIdeaScalaVersion {
  // Axis Filter to avoid loading JS or Native into IDE
  private val jvmOnly = MatrixAction
    .ForPlatforms(VirtualAxis.js, VirtualAxis.native)
    .Configure(_.settings(ideSkipProject := true))

  implicit final class ProjectMatrixSyntaxExtension(val pm: ProjectMatrix) extends AnyVal {

    def jvmOnly(scalaVersion: IntellijIdeaScalaVersion, excluding: PartialFunction[String, Unit] = PartialFunction.empty): ProjectMatrix = {
      val scalaVersions = allScalaVersions(excluding.lift.andThen(_.isDefined))
      pm.someVariations(scalaVersions = scalaVersions, List(VirtualAxis.jvm))(
        scalaVersion.oneAxisAtATimeExportedTpIntellijIdea,
        IntellijIdeaScalaVersion.jvmOnly
      )
    }
  }
}