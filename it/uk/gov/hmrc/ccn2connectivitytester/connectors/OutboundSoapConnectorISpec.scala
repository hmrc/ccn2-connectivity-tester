/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.ccn2connectivitytester.models.SendingStatus
import uk.gov.hmrc.ccn2connectivitytester.models.common.Requests
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class OutboundSoapConnectorISpec extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite with WireMockSupport with WiremockTestSupport {
  val requests = app.injector.instanceOf[Requests]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  trait Setup {
    val underTest: OutboundSoapConnector = app.injector.instanceOf[OutboundSoapConnector]
    val messageId                        = "messageId"
    implicit val hc: HeaderCarrier       = HeaderCarrier()
    val globalId                         = UUID.randomUUID()
  }

  "postMessage" should {
    Seq("v1" -> requests.getV1Request, "v2" -> requests.getV2Request) foreach { version =>
      s"handle success for ${version._1} messages" in new Setup {
        val httpStatus      = ACCEPTED
        val successResponse =
          s"""{
             |"globalId":"$globalId",
             |"messageId": "$messageId",
             |"status" : "SENT",
             |"ccnHttpStatus" : $httpStatus
             |}
             |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, successResponse)
        val response        = await(underTest.sendRequestAndProcessResponse(version._2, wireMockUrl))
        response.isRight shouldBe true
        response.map(sms => {
          sms.messageId shouldBe messageId
          sms.globalId shouldBe globalId
          sms.ccnHttpStatus shouldBe httpStatus
          sms.status shouldBe SendingStatus.SENT
        })
      }
    }

    Seq("v1" -> requests.getV1Request, "v2" -> requests.getV2Request) foreach { version =>
      s"handle retrying for ${version._1} messages" in new Setup {
        val httpStatus       = PERMANENT_REDIRECT
        val retryingResponse =
          s"""{
             |"globalId":"$globalId",
             |"messageId": "$messageId",
             |"status" : "RETRYING",
             |"ccnHttpStatus" : $httpStatus
             |}
             |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, retryingResponse)
        val response         = await(underTest.sendRequestAndProcessResponse(version._2, wireMockUrl))
        response.isRight shouldBe true
        response.map(sms => {
          sms.messageId shouldBe messageId
          sms.globalId shouldBe globalId
          sms.ccnHttpStatus shouldBe httpStatus
          sms.status shouldBe SendingStatus.RETRYING
        })
      }
    }

    Seq("v1" -> requests.getV1Request, "v2" -> requests.getV2Request) foreach { version =>
      s"handle failed for ${version._1} messages" in new Setup {
        val httpStatus   = INTERNAL_SERVER_ERROR
        val failResponse =
          s"""{
             |"globalId":"$globalId",
             |"messageId": "$messageId",
             |"status" : "FAILED",
             |"ccnHttpStatus" : $httpStatus
             |}
             |""".stripMargin
        setupPostForCCNWithResponseBody("/message", 200, failResponse)
        val response     = await(underTest.sendRequestAndProcessResponse(version._2, wireMockUrl))
        response.isRight shouldBe true
        response.map(sms => {
          sms.messageId shouldBe messageId
          sms.globalId shouldBe globalId
          sms.ccnHttpStatus shouldBe httpStatus
          sms.status shouldBe SendingStatus.FAILED
        })
      }
    }

    Seq("V1-request", "V2-request") foreach { version =>
      s"handle NotFound for $version messages" in new Setup {
        setupPostForCCN("/message", 404)
        val response = await(underTest.sendRequestAndProcessResponse(version, wireMockUrl))
        response.isLeft shouldBe true
      }
    }
  }
}
