import sbtghactions.GenerativePlugin.autoImport.UseRef
import sbtghactions.GenerativePlugin.autoImport.WorkflowStep

object CustomGithubActions {

  lazy val generateCC = WorkflowStep.Sbt(
    name = Some("Generate Code Coverage Reports"),
    commands = List("clean", "coverage", "test"),
    cond = Some(s"matrix.scala == '${ScalaVersions.scala213}'")
  )

  lazy val aggregateCC = WorkflowStep.Sbt(
    name = Some("Aggregate Code Coverage Report"),
    commands = List("coverageAggregate"),
    cond = Some(s"matrix.scala == '${ScalaVersions.scala213}'")
  )

  lazy val uploadCC = WorkflowStep.Use(
    name = Some("Upload Code Coverage Report"),
    ref = UseRef.Public("codecov", "codecov-action", "v3"),
    cond = Some(s"matrix.scala == '${ScalaVersions.scala213}'"),
    params = Map(
      "token" -> "${{ secrets.CODECOV_TOKEN }}"
    )
  )
}
