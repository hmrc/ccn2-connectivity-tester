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
import scala.concurrent.duration.{FiniteDuration, SECONDS}

import org.mockito.ArgumentMatchers.{any as `*`, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.apiplatform.modules.common.utils.{FixedClock, HmrcSpec}
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.common.SuccessResult
import uk.gov.hmrc.ccn2connectivitytester.services.OutboundService
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}

class LockedScheduledJobSpec extends HmrcSpec with ScalaFutures with GuiceOneAppPerTest with BeforeAndAfterEach with FixedClock with MockitoSugar {

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup {
    val mockOutboundService = mock[OutboundService]
    val mockLockRepository  = mock[MongoLockRepository]
    val mockAppConfig       = mock[AppConfig]
    when(mockAppConfig.scheduledJobEnabled).thenReturn(true)
    when(mockAppConfig.checkInterval).thenReturn(FiniteDuration(5, SECONDS))
    when(mockAppConfig.checkInitialDelay).thenReturn(FiniteDuration(5, SECONDS))

    val subject = new SendV2SoapMessageJob(mockAppConfig, mockLockRepository, mockOutboundService)
  }

  "ExclusiveScheduledJob" should {

    "back off when Mongo lock cannot be obtained" in new Setup {
      when(mockLockRepository.takeLock(*, *, *)).thenReturn(Future.successful(None))

      val result = await(subject.execute)

      result.message shouldBe "Job named Scheduled Job sending V2 SOAP messages cannot acquire Mongo lock, not running"
      verify(mockLockRepository).takeLock(eqTo("Scheduled Job sending V2 SOAP messages-lock"), *, *)
      verify(mockOutboundService, never).sendTestMessage()
    }

    "execute in lock when Mongo lock can be obtained" in new Setup {
      when(mockLockRepository.takeLock(*, *, *)).thenReturn(Future.successful(Some(Lock("", "", instant, instant))))
      when(mockLockRepository.releaseLock(*, *)).thenReturn(Future.successful(()))
      when(mockOutboundService.sendTestMessage()).thenReturn(Future(SuccessResult))

      val result = await(subject.execute)

      result.message shouldBe "Job named Scheduled Job sending V2 SOAP messages ran and completed with result SuccessResult"
      verify(mockLockRepository).takeLock(eqTo("Scheduled Job sending V2 SOAP messages-lock"), *, *)
      verify(mockLockRepository).releaseLock(eqTo("Scheduled Job sending V2 SOAP messages-lock"), *)
    }
  }
}
