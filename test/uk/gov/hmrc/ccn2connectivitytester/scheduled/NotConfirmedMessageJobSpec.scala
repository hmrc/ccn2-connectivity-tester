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

import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Source.fromIterator
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, SoapMessageStatus}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import scala.concurrent.duration.{Duration, FiniteDuration}

class NotConfirmedMessageJobSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite
  with MockitoSugar with ArgumentMatchersSugar {

  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()

  trait Setup {
    val appConfigMock: AppConfig = mock[AppConfig]
    val mockMongoRepository: SoapMessageStatusRepository = mock[SoapMessageStatusRepository]
    val mongoLockRepository: MongoLockRepository = mock[MongoLockRepository]
  }

  "NotConfirmedMessageJob" should {
    "process messages that have not received confirmations" in new Setup {
      val message = new SoapMessageStatus(UUID.randomUUID(), "some id", SendingStatus.SENT, ACCEPTED)
      when(appConfigMock.parallelism).thenReturn(2)
      when(appConfigMock.checkJobLockDuration).thenReturn(Duration("5s"))
      when(appConfigMock.checkInterval).thenReturn(Duration("5s"))
      when(mongoLockRepository.takeLock(*, *, *)).thenReturn(successful(true))
      when(mongoLockRepository.releaseLock(*, *)).thenReturn(successful(()))
      when(appConfigMock.checkJobLockDuration).thenReturn(FiniteDuration(60, "secs"))
      when(mockMongoRepository.retrieveMessagesMissingConfirmation).
      thenReturn(Source.future(successful(message)))

      val underTest = new NotConfirmedMessageJob(appConfigMock, mongoLockRepository, mockMongoRepository)
      val result: underTest.Result = await(underTest.execute)

      result.message shouldBe "Job named NotConfirmedMessageJob ran and completed with result OK"
      verify(mockMongoRepository).retrieveMessagesMissingConfirmation
      verifyNoMoreInteractions(mockMongoRepository)
    }
  }

}
