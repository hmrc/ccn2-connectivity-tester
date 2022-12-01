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

package uk.gov.hmrc.ccn2connectivitytester.services

import java.util.UUID

import com.mongodb.client.result.InsertOneResult
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mongodb.scala.bson.BsonNumber
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.mvc.Http.Status.ACCEPTED
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models._
import uk.gov.hmrc.ccn2connectivitytester.models.common.Version.{V1, V2}
import uk.gov.hmrc.ccn2connectivitytester.models.common.{FailResult, Requests, SuccessResult}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class OutboundServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()

  trait Setup {
    val repoMock = mock[SoapMessageStatusRepository]
    val connectorMock = mock[OutboundSoapConnector]
    val requestsMock = mock[Requests]
    val configMock = mock[AppConfig]
    val underTest = new OutboundService(connectorMock, repoMock, requestsMock)
    val uri = "https://dummy.com"
    when(configMock.outboundSoapUrl).thenReturn(uri)
  }

  "sendMessage" should {
    "successfully send a V1 message" in new Setup {
      val successResponse = new SoapMessageStatus(UUID.randomUUID(), "message Id", SendingStatus.SENT, ACCEPTED)
      when(requestsMock.getV1Request).thenReturn("blah")
      when(connectorMock.sendRequestAndProcessResponse(*, *)).thenReturn(Future(Right(successResponse)))
      when(repoMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))

      val result = await(underTest.sendTestMessage(V1))

      result shouldBe SuccessResult
      verify(connectorMock).sendRequestAndProcessResponse("blah", "")
      verify(repoMock).persist(successResponse)
      verify(requestsMock).getV1Request
      verifyNoMoreInteractions(requestsMock)
      verifyNoMoreInteractions(repoMock)
    }

    "handle failing to send a message" in new Setup {
      when(requestsMock.getV1Request).thenReturn("blah")
      when(connectorMock.sendRequestAndProcessResponse(*, *)).
        thenReturn(successful(Left(UpstreamErrorResponse("unexpected error", INTERNAL_SERVER_ERROR))))

      val result = await(underTest.sendTestMessage(V1))

      result shouldBe FailResult
      verify(connectorMock).sendRequestAndProcessResponse("blah", "")
      verify(requestsMock).getV1Request
      verifyNoMoreInteractions(requestsMock)
      verifyNoMoreInteractions(repoMock)
    }

    "successfully send a V2 message" in new Setup {
      val successResponse = new SoapMessageStatus(UUID.randomUUID(), "message Id", SendingStatus.SENT, ACCEPTED)
      when(requestsMock.getV2Request).thenReturn("blah")
      when(connectorMock.sendRequestAndProcessResponse(*, *)).thenReturn(Future(Right(successResponse)))
      when(repoMock.persist(*)).thenReturn(Future(InsertOneResult.acknowledged(BsonNumber(1))))

      val result = await(underTest.sendTestMessage(V2))

      result shouldBe SuccessResult
      verify(connectorMock).sendRequestAndProcessResponse("blah", "")
      verify(repoMock).persist(successResponse)
      verify(requestsMock).getV2Request
      verifyNoMoreInteractions(requestsMock)
      verifyNoMoreInteractions(repoMock)
    }

  }
}
