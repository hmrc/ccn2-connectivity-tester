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

package uk.gov.hmrc.ccn2connectivitytester.connectors

import java.net.URL
import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus
import uk.gov.hmrc.ccn2connectivitytester.models.common.{FailResult, SendResult, SuccessResult}
import uk.gov.hmrc.http.HttpReadsInstances.{readEitherOf, readFromJson}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton()
class Ccn2Connector @Inject()(val config: AppConfig, val http: HttpClient)(implicit ec: ExecutionContext) extends Logging{
 val v1Request =
  s"""
     |{"wsdlUrl":"https://import-control-wsdls.protected.mdtp/assets/eu/outbound/CR-for-NES-Services/BusinessActivityService/ICS/RiskAnalysisOrchestrationBAS/V1/CCN2.Service.Customs.Default.ICS.RiskAnalysisOrchestrationBAS_1.0.0_CCN2_1.0.0.wsdl",
     | "wsdlOperation":"IsAlive", "messageBody":"", "confirmationOfDelivery": true,
     | "addressing": {
     |  "to":"partner:CCN2.Partner.EU.Customs.TAXUD/ICS_CR.CONF",
     |  "messageId":"ISALIVE-${Instant.now()}"
     |  }
     |}
    """.stripMargin
private val v2Request =
  s"""
    |{"wsdlUrl":"https://import-control-wsdls.protected.mdtp/assets/eu/outbound/CR-for-NES-Services-V2/BusinessActivityService/ICS/RiskAnalysisOrchestrationBAS/V2/CCN2.Service.Customs.EU.ICS.RiskAnalysisOrchestrationBAS_2.0.0_CCN2_2.0.0.wsdl",
    | "wsdlOperation":"IsAlive", "messageBody":"", "confirmationOfDelivery": true,
    | "addressing": {
    |   "to":"partner:CCN2.Partner.EU.Customs.TAXUD/ICS_CR_V2.CONF",
    |   "from":"partner:CCN2.Partner.XI.Customs.TAXUD/ICS_NES_V2.CONF",
    |   "replyTo":"partner:CCN2.Partner.XI.Customs.TAXUD/ICS_NES_V2.CONF",
    |   "faultTo":"partner:CCN2.Partner.XI.Customs.TAXUD/ICS_NES_V2.CONF",
    |   "messageId":"ISALIVE-${Instant.now()}"
    | }
    |}
    """.stripMargin

  def sendRequest(version: String, destinationUrl: String)(implicit hc: HeaderCarrier ): Future[SendResult] =  {
   sendVersion1Request(destinationUrl).map {
     case Left(UpstreamErrorResponse(_, statusCode, _, _)) =>
       logger.warn(s"The request was not sent. The service returned $statusCode")
       FailResult
     case Right(response: SoapMessageStatus) =>
       logger.info(s"Status is ${response.status}")
       SuccessResult
    }
     .recoverWith {
     case NonFatal(e) =>
       logger.warn (s"Unable to send request to CCN2: ${e.getMessage}")
       Future.successful(FailResult)
   }
  }

  /*private def getStatusFromResponse(response: JsValue) = {
    response \ "status" match {
      case "SENT" =>
    }

  }*/

  private def sendVersion1Request(destinationUrl: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, SoapMessageStatus]] = {
    http.POSTString[Either[UpstreamErrorResponse, SoapMessageStatus]](new URL(s"$destinationUrl/message"), v1Request)
  }

  private def sendVersion2Request(destinationUrl: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, SoapMessageStatus]] = {
    http.POSTString[Either[UpstreamErrorResponse, SoapMessageStatus]](new URL(s"$destinationUrl/message"), v2Request)
  }
}
