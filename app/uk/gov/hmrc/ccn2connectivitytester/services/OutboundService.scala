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
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import uk.gov.hmrc.http.{HttpErrorFunctions, UpstreamErrorResponse}

import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models.common._
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository

@Singleton
class OutboundService @Inject() (
    outboundConnector: OutboundSoapConnector,
    soapMessageStatusRepository: SoapMessageStatusRepository,
    requests: Requests
  )(implicit val ec: ExecutionContext
  ) extends HttpErrorFunctions with Logging {

  def sendTestMessage(): Future[SendResult] = {
    val requestToSend = requests.getV2Request

    outboundConnector.sendOutboundSoapRequest(requestToSend) map {
      case Right(soapMessageStatus)                               =>
        soapMessageStatusRepository.persist(soapMessageStatus)
        logger.info(s"Sending message with messageId [${soapMessageStatus.messageId}] received response code of ${soapMessageStatus.ccnHttpStatus}")
        SuccessResult
      case Left(UpstreamErrorResponse(message, statusCode, _, _)) =>
        logger.warn(s"Error $message and status code $statusCode")
        FailResult
    }
  }
}
