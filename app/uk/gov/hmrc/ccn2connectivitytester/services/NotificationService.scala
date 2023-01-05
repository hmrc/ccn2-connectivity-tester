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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.ccn2connectivitytester.models.common.{MessageIdNotFoundResult, UpdateResult, UpdateSuccessResult}
import uk.gov.hmrc.ccn2connectivitytester.models.{SendingStatus, SoapMessageStatus}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(soapMessageStatusRepository: SoapMessageStatusRepository)
                                   (implicit val ec: ExecutionContext) {
  def processNotification(msgId: String, status: SendingStatus): Future[UpdateResult] = {

    def doUpdate(id: String, status: SendingStatus): Future[UpdateSuccessResult.type] = {
      soapMessageStatusRepository.updateSendingStatus(id, status) map {
        case _ => UpdateSuccessResult
      }
    }

    soapMessageStatusRepository.findById(msgId).flatMap {
      case None => Future.successful(MessageIdNotFoundResult)
      case Some(_: SoapMessageStatus) => doUpdate(msgId, status)
    }
  }
}
