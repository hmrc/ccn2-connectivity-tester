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

import javax.inject.{Inject, Singleton}
import org.joda.time.Duration
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models.common.Version.V1
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendV2SoapMessageJob @Inject()(appConfig: AppConfig, override val lockRepository: MongoLockRepository,
                                     outboundSoapConnector: OutboundSoapConnector)
  extends LockedScheduledJob {
  override val releaseLockAfter: Duration = Duration.standardSeconds(appConfig.checkJobLockDuration.toSeconds)

  override def name: String = "SendV2SoapMessageJob"

  override def initialDelay: FiniteDuration = appConfig.checkInitialDelay.asInstanceOf[FiniteDuration]

  override def interval: FiniteDuration = appConfig.checkInterval.asInstanceOf[FiniteDuration]

  override def executeInLock(implicit ec: ExecutionContext): Future[Result] = {
    outboundSoapConnector.sendRequest(V1, appConfig.outboundSoapUri).map(done => Result(done.toString))
  }
}
