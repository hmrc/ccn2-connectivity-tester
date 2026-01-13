import sbt._

object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  private val bootstrapVersion    = "10.5.0"
  private val hmrcMongoVersion    = "2.11.0"
  private val commonDomainVersion = "0.19.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "api-platform-common-domain"  % commonDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion,
    "org.mockito"             %% "mockito-scala-scalatest"    % "2.0.0"
  ).map(_ % "test")
}
