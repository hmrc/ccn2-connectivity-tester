import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.14.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"          % hmrcMongoVersion,
    "com.lightbend.akka"      %% "akka-stream-alpakka-mongodb" % "4.0.0",
    "com.beachape"            %% "enumeratum-play-json"        % "1.7.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % hmrcMongoVersion,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.29",
    "org.scalatest"           %% "scalatest"                  % "3.2.17",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.35.0",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.62.2"
  ).map(_ % "test, it")
}
