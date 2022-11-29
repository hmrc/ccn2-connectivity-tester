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

package uk.gov.hmrc.ccn2connectivitytester.services

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.ccn2connectivitytester.connectors.OutboundSoapConnector
import uk.gov.hmrc.ccn2connectivitytester.models.common.{FailResult, Requests, SendResult, SuccessResult, Version}
import uk.gov.hmrc.ccn2connectivitytester.repositories.SoapMessageStatusRepository
import uk.gov.hmrc.http.{HttpErrorFunctions, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OutboundService @Inject()(outboundConnector: OutboundSoapConnector,
                                soapMessageStatusRepository: SoapMessageStatusRepository,
                                requests: Requests)
                               (implicit val ec: ExecutionContext)
  extends HttpErrorFunctions with Logging {

  def sendTestMessage(version: Version): Future[SendResult] = {
    val requestToSend = version match {
      case Version.V1 => requests.getV1Request
      case Version.V2 => requests.getV2Request
    }

    outboundConnector.sendRequestAndProcessResponse(requestToSend) map { response =>
      response match {
        case Right(soapMessageStatus) => soapMessageStatusRepository.persist(soapMessageStatus)
          SuccessResult
        case Left(UpstreamErrorResponse(message, statusCode, _, _)) => logger.warn(s"Error $message and status code $statusCode")
          FailResult
        case _ => logger.warn("Unhandled error when calling api-platform-outbound-soap")
          FailResult
      }
    }
  }
}
