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

package uk.gov.hmrc.ccn2connectivitytester.services

import java.util.UUID

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.ccn2connectivitytester.models.common.{MessageIdNotFoundResult, UpdateSuccessResult}
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, SoapMessageStatus}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class NotificationServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()
  trait Setup {
    val mockRepository = mock[SoapMessageStatusRepository]
    val underTest = new NotificationService(mockRepository)
  }


  "processNotification" should {
    "update to SENT message that exists" in new Setup {
      val messageId = "some message ID"
      val newStatus = SendingStatus.SENT
      val soapMessageStatus = new SoapMessageStatus(UUID.randomUUID(), messageId, SendingStatus.RETRYING, ACCEPTED)
      when(mockRepository.findById(messageId)).thenReturn(successful(Some(soapMessageStatus)))
      when(mockRepository.updateSendingStatus(messageId, newStatus)).thenReturn(successful(Some(soapMessageStatus)))

      val result = await(underTest.processNotification(messageId, newStatus))

      result shouldBe UpdateSuccessResult
      verify(mockRepository).updateSendingStatus(messageId, newStatus)
    }

    "update to FAILED message that exists" in new Setup {
      val messageId = "some message ID"
      val newStatus = SendingStatus.FAILED
      val soapMessageStatus = new SoapMessageStatus(UUID.randomUUID(), messageId, SendingStatus.RETRYING, ACCEPTED)
      when(mockRepository.findById(messageId)).thenReturn(successful(Some(soapMessageStatus)))
      when(mockRepository.updateSendingStatus(messageId, newStatus)).thenReturn(successful(Some(soapMessageStatus)))

      val result = await(underTest.processNotification(messageId, newStatus))

      result shouldBe UpdateSuccessResult
      verify(mockRepository).updateSendingStatus(messageId, newStatus)
    }

    "update to CoD message that exists" in new Setup {
      val messageId = "some message ID"
      val newStatus = SendingStatus.COD
      val soapMessageStatus = new SoapMessageStatus(UUID.randomUUID(), messageId, SendingStatus.SENT, ACCEPTED)
      when(mockRepository.findById(messageId)).thenReturn(successful(Some(soapMessageStatus)))
      when(mockRepository.updateSendingStatus(messageId, newStatus)).thenReturn(successful(Some(soapMessageStatus)))

      val result = await(underTest.processNotification(messageId, newStatus))

      result shouldBe UpdateSuccessResult
      verify(mockRepository).updateSendingStatus(messageId, newStatus)
    }

    "update to CoE message that exists" in new Setup {
      val messageId = "some message ID"
      val newStatus = SendingStatus.COE

      val soapMessageStatus = new SoapMessageStatus(UUID.randomUUID(), messageId, SendingStatus.SENT, ACCEPTED)
      when(mockRepository.findById(messageId)).thenReturn(successful(Some(soapMessageStatus)))
      when(mockRepository.updateSendingStatus(messageId, newStatus)).thenReturn(successful(Some(soapMessageStatus)))

      val result = await(underTest.processNotification(messageId, newStatus))

      result shouldBe UpdateSuccessResult
      verify(mockRepository).updateSendingStatus(messageId, newStatus)
    }

    "not update a message that does not exist" in new Setup {
      val messageId = "some message ID"
      when(mockRepository.findById(messageId)).thenReturn(successful(Option.empty))
      when(mockRepository.updateSendingStatus(messageId, SendingStatus.COD)).thenReturn(successful(Option.empty))

      val result = await(underTest.processNotification(messageId, SendingStatus.COD))

      result shouldBe MessageIdNotFoundResult
      verify(mockRepository).findById(messageId)
      verifyNoMoreInteractions(mockRepository)
    }
  }

}
