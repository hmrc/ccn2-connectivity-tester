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

package uk.gov.hmrc.ccn2connectivitytester.scheduled

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models.common.{SuccessResult, Version}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.concurrent.duration.FiniteDuration

class SendV1SoapMessageJobTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite
  with MockitoSugar with ArgumentMatchersSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()

  trait Setup {
    val appConfigMock: AppConfig = mock[AppConfig]
    val outboundSoapConnector: OutboundSoapConnector = mock[OutboundSoapConnector]
    val mongoLockRepository: MongoLockRepository = mock[MongoLockRepository]
    val mockOutboundSoapUri: String = "https://dummy.com"
  }

  "SendV1SoapMessageJobTest" should {
    "invoke sending V1 message on OutboundSoapConnector" in new Setup {
      when(mongoLockRepository.takeLock(*,*,*)).thenReturn(successful(true))
      when(mongoLockRepository.releaseLock(*,*)).thenReturn(successful(()))

      when(appConfigMock.checkJobLockDuration).thenReturn(FiniteDuration(60, "secs"))
      when(appConfigMock.outboundSoapUri).thenReturn(mockOutboundSoapUri)
      when(outboundSoapConnector.sendRequest(Version.V1, mockOutboundSoapUri)) thenReturn (Future(SuccessResult))
      val underTest = new SendV1SoapMessageJob(appConfigMock, mongoLockRepository, outboundSoapConnector)
      val result: underTest.Result = await(underTest.execute)
      result.message shouldBe "Job named SendV1SoapMessageJob ran and completed with result SuccessResult"
    }
  }

}