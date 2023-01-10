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

import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import play.api.Application
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext

class SchedulerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}

@Singleton
class Scheduler @Inject() (
    override val applicationLifecycle: ApplicationLifecycle,
    override val application: Application,
    sendV1MessageJob: SendV1SoapMessageJob,
    sendV2MessageJob: SendV2SoapMessageJob,
    notConfirmedMessageJob: NotConfirmedMessageJob
  )(
    override implicit val ec: ExecutionContext
  ) extends RunningOfScheduledJobs {
  override lazy val scheduledJobs: Seq[LockedScheduledJob] = Seq(sendV1MessageJob, sendV2MessageJob, notConfirmedMessageJob)
}
