import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.12.0"
  private val hmrcMongoVersion = "0.74.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"          % hmrcMongoVersion,
    "com.lightbend.akka"      %% "akka-stream-alpakka-mongodb" % "4.0.0",
    "com.beachape"            %% "enumeratum-play-json"        % "1.7.0"

  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion            % "it",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"                     % "test, it",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.12"                   % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.34.0"                    % "it"

  )
}
