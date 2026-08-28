import sbt._

object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  private val bootstrapVersion    = "10.8.0"
  private val hmrcMongoVersion    = "2.13.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "api-platform-common-domain"  % "1.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion,
    "org.mockito"             %% "mockito-scala-scalatest"    % "2.2.1",
    "uk.gov.hmrc"           %% "api-platform-common-domain-fixtures" % "1.0.0"

  ).map(_ % "test")
}
