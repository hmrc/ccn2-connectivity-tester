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

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotConfirmedMessageJob @Inject()(appConfig: AppConfig, override val lockRepository: MongoLockRepository,
                                       soapMessageStatusRepository: SoapMessageStatusRepository)
                                      (implicit val ec: ExecutionContext, mat: Materializer)

  extends LockedScheduledJob with Logging {
  override val releaseLockAfter: FiniteDuration = appConfig.checkJobLockDuration.asInstanceOf[FiniteDuration]

  override def name: String = "NotConfirmedMessageJob"

  override def initialDelay: FiniteDuration = appConfig.checkInitialDelay.asInstanceOf[FiniteDuration]

  override def interval: FiniteDuration = appConfig.checkInterval.asInstanceOf[FiniteDuration]

  override def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    def logMessageDetails(soapMessageStatus: SoapMessageStatus) = {
      logger.warn(s"Message with messageId [${soapMessageStatus.messageId}] has not received confirmation of delivery")
      Future.unit
    }

    soapMessageStatusRepository.retrieveMessagesMissingConfirmation.runWith(Sink.foreachAsync[SoapMessageStatus](appConfig.parallelism)(logMessageDetails)).map(done => Result("OK"))
  }
}
