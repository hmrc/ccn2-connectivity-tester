/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ccn2connectivitytester.connectors

import java.util.UUID

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneServerPerSuite}
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.ccn2connectivitytester.models.common.{FailResult, SuccessResult, Version}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class Ccn2ConnectorISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite with WireMockSupport with WiremockTestSupport {
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  trait Setup {
    val underTest: Ccn2Connector = app.injector.instanceOf[Ccn2Connector]
    val messageId = "messageId"
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "postMessage" should {
    Seq(Version.V1, Version.V2) foreach { version =>
      s"handleSuccess for $version messages" in new Setup {
        val successResponse =
          s"""{
            |"globalId":"${UUID.randomUUID()}",
            |"messageId": "${UUID.randomUUID()}",
            |"status" : "SENT",
            |"ccnHttpStatus" : 202
            |}
            |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, successResponse)
        val response = await(underTest.sendRequest(version, wireMockUrl))
        response shouldBe SuccessResult
      }
    }

    Seq(Version.V1, Version.V2) foreach { version =>
      s"handleRetrying for $version messages" in new Setup {
        val retryingResponse =
          s"""{
            |"globalId":"${UUID.randomUUID()}",
            |"messageId": "${UUID.randomUUID()}",
            |"status" : "RETRYING",
            |"ccnHttpStatus" : 301
            |}
            |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, retryingResponse)
        val response = await(underTest.sendRequest(version, wireMockUrl))
        response shouldBe FailResult
      }
    }

    Seq(Version.V1, Version.V2) foreach { version =>
      s"handleFailed for $version messages" in new Setup {
        val failResponse =
          s"""{
            |"globalId":"${UUID.randomUUID()}",
            |"messageId": "${UUID.randomUUID()}",
            |"status" : "FAILED",
            |"ccnHttpStatus" : 500
            |}
            |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, failResponse)
        val response = await(underTest.sendRequest(version, wireMockUrl))
        response shouldBe FailResult
      }
    }

    Seq(Version.V1, Version.V2) foreach { version =>
      s"handleNotFound for $version messages" in new Setup {
        setupPostForCCN("/message", 404)
        val response = await(underTest.sendRequest(version, wireMockUrl))
        response shouldBe FailResult
      }
    }
  }
}
