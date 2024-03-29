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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus
import uk.gov.hmrc.ccn2connectivitytester.models.common._
import uk.gov.hmrc.ccn2connectivitytester.services.NotificationService

@Singleton
class NotificationController @Inject() (cc: ControllerComponents, notificationService: NotificationService)(implicit ec: ExecutionContext)
    extends BackendController(cc) with Logging {

  def message: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SoapMessageStatus] { messageRequest =>
      notificationService.processNotification(messageRequest.messageId, messageRequest.status) map {
        case UpdateSuccessResult     =>
          logger.info(s"Received notification for messageId ${messageRequest.messageId}. Status is ${messageRequest.status}")
          Ok
        case MessageIdNotFoundResult =>
          logger.warn(s"Received notification for unknown ID $messageRequest.messageId")
          NotFound
      }
    }
  }
}
