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

package uk.gov.hmrc.ccn2connectivitytester.scheduled

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.concurrent.duration.FiniteDuration

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models.common.SuccessResult
import uk.gov.hmrc.ccn2connectivitytester.models.common.Version.V2
import uk.gov.hmrc.ccn2connectivitytester.services.OutboundService

class SendV2SoapMessageJobTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite
    with MockitoSugar with ArgumentMatchersSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()

  trait Setup {
    val appConfigMock: AppConfig                     = mock[AppConfig]
    val outboundSoapConnector: OutboundSoapConnector = mock[OutboundSoapConnector]
    val mongoLockRepository: MongoLockRepository     = mock[MongoLockRepository]
    val mockOutboundService: OutboundService         = mock[OutboundService]
  }

  "SendV2SoapMessageJobTest" should {
    "invoke sending V2 message on OutboundSoapConnector" in new Setup {
      when(mongoLockRepository.takeLock(*, *, *)).thenReturn(successful(true))
      when(mongoLockRepository.releaseLock(*, *)).thenReturn(successful(()))
      when(mockOutboundService.sendTestMessage(*)).thenReturn(successful(SuccessResult))

      when(appConfigMock.checkJobLockDuration).thenReturn(FiniteDuration(60, "secs"))
      when(mockOutboundService.sendTestMessage(V2)) thenReturn Future(SuccessResult)
      val underTest                = new SendV2SoapMessageJob(appConfigMock, mongoLockRepository, mockOutboundService)
      val result: underTest.Result = await(underTest.execute)
      result.message shouldBe "Job named Scheduled Job sending V2 SOAP messages ran and completed with result SuccessResult"
      verify(mockOutboundService).sendTestMessage(V2)
    }
  }
}
