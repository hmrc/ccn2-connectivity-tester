import sbt._

object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  private val bootstrapVersion    = "9.11.0"
  private val hmrcMongoVersion    = "2.5.0"
  private val commonDomainVersion = "0.18.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"          % hmrcMongoVersion,
    "org.apache.pekko"        %% "pekko-connectors-mongodb"    % "1.0.2",
    "uk.gov.hmrc"             %% "api-platform-common-domain"  % commonDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion,
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.30"
  ).map(_ % "test")
}
