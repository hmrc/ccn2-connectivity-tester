import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.8.0"
  private val hmrcMongoVersion = "0.73.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % hmrcMongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion            % Test,
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.34.0"                    % "it"

  )
}
