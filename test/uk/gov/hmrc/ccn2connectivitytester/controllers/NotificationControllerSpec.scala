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

package uk.gov.hmrc.ccn2connectivitytester.controllers

import java.util.UUID

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.ccn2connectivitytester.models.SendingStatus
import uk.gov.hmrc.ccn2connectivitytester.models.common.{MessageIdNotFoundResult, UpdateSuccessResult}
import uk.gov.hmrc.ccn2connectivitytester.services.NotificationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class NotificationControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false
      )
      .build()

  "message" should {
    val fakeRequest = FakeRequest("POST", "/notification")
    trait Setup {
      val mockNotificationService = mock[NotificationService]
      val underTest = new NotificationController(Helpers.stubControllerComponents(), mockNotificationService)
    }

    "message" should {
      "return success when processing notification" in new Setup {
        val messageId = "id"
        val message = Json.obj(
          "globalId" -> UUID.randomUUID(),
          "messageId" -> messageId,
          "status" -> "SENT",
          "ccnHttpStatus" -> 202)
        when(mockNotificationService.processNotification(*, *)).thenReturn(successful(UpdateSuccessResult))

        val result = underTest.message()(fakeRequest.withBody(message))

        status(result) shouldBe OK
        verify(mockNotificationService).processNotification(messageId, SendingStatus.SENT)
      }

      "return message not found when processing notification for message ID not known" in new Setup {
        val messageId = "id"
        val message = Json.obj(
          "globalId" -> UUID.randomUUID(),
          "messageId" -> messageId,
          "status" -> "SENT",
          "ccnHttpStatus" -> 202)
        when(mockNotificationService.processNotification(*, *)).thenReturn(successful(MessageIdNotFoundResult))

        val result = underTest.message()(fakeRequest.withBody(message))

        status(result) shouldBe NOT_FOUND
        verify(mockNotificationService).processNotification(messageId, SendingStatus.SENT)
      }
    }
  }
}
