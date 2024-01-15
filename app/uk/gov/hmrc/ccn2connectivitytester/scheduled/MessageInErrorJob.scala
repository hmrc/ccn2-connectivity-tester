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

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

import akka.stream.Materializer
import akka.stream.scaladsl.Sink

import play.api.Logging
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, SoapMessageStatus}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository

@Singleton
class MessageInErrorJob @Inject() (
    appConfig: AppConfig,
    override val lockRepository: MongoLockRepository,
    soapMessageStatusRepository: SoapMessageStatusRepository
  )(implicit val ec: ExecutionContext,
    mat: Materializer
  ) extends LockedScheduledJob with Logging {
  override val releaseLockAfter: FiniteDuration = appConfig.checkJobLockDuration.asInstanceOf[FiniteDuration]

  override def name: String = "Scheduled Job seeking messages in error state"

  override def initialDelay: FiniteDuration = appConfig.checkInitialDelay.asInstanceOf[FiniteDuration]

  override def interval: FiniteDuration = appConfig.checkInterval.asInstanceOf[FiniteDuration]

  override def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    def logMessageDetails(soapMessageStatus: SoapMessageStatus) = {
      soapMessageStatusRepository.updateSendingStatus(soapMessageStatus.messageId, SendingStatus.ALERTED)
      logger.warn(s"Message with messageId [${soapMessageStatus.messageId}] is in a state of ${soapMessageStatus.status} " +
        s"and it has not been possible to deliver it since it was created at ${soapMessageStatus.createDateTime}")
      Future.unit
    }

    soapMessageStatusRepository.retrieveMessagesInErrorState.runWith(
      Sink.foreachAsync[SoapMessageStatus](appConfig.parallelism)(logMessageDetails)
    ).map(done => Result("OK"))
  }
}
