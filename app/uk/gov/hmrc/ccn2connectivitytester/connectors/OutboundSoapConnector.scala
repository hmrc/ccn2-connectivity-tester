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
import play.api.http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus
import uk.gov.hmrc.ccn2connectivitytester.models.common.{FailResult, SendResult, SuccessResult, Version}
import uk.gov.hmrc.http.HttpReadsInstances.{readEitherOf, readFromJson}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class OutboundSoapConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) extends Logging {

  val v1Request =
    s"""
       |{"wsdlUrl":"https://import-control-wsdls.protected.mdtp/assets/eu/outbound/CR-for-NES-Services/BusinessActivityService/ICS/RiskAnalysisOrchestrationBAS/V1/CCN2.Service.Customs.Default.ICS.RiskAnalysisOrchestrationBAS_1.0.0_CCN2_1.0.0.wsdl",
       | "wsdlOperation":"IsAlive", "messageBody":"", "confirmationOfDelivery": true,
       | "addressing": {
       |  "to":"partner:CCN2.Partner.EU.Customs.TAXUD/ICS_CR.CONF",
       |  "messageId":"ISALIVE-${Instant.now().getEpochSecond()}"
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
       |   "messageId":"ISALIVE-${Instant.now().getEpochSecond()}"
       | }
       |}
    """.stripMargin

  def sendRequest(version: Version, destinationUrl: String): Future[SendResult] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    def handleErrorResponse(statusCode: Int) = {
      logger.warn(s"The request was not sent. The service returned $statusCode")
      FailResult
    }

    def getResultFromResponse(response: SoapMessageStatus): SendResult = {
      response.status match {
        case "SENT" => SuccessResult
        case _ => FailResult
      }
    }

    version match {
      case Version.V1 => sendRequest(destinationUrl, v1Request) map { response =>
          response match {
            case Right(soapMessageStatus) => getResultFromResponse(soapMessageStatus)
            case Left(UpstreamErrorResponse(_, statusCode, _, _)) => handleErrorResponse(statusCode)
          }
        }

      case Version.V2 => sendRequest(destinationUrl, v2Request) map { response =>
        response match {
          case Right(soapMessageStatus) => getResultFromResponse(soapMessageStatus)
          case Left(UpstreamErrorResponse(_, statusCode, _, _)) => handleErrorResponse(statusCode)
        }
      }
    }
  }

  private def sendRequest(destinationUrl: String, request: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, SoapMessageStatus]] = {
    httpClient.POSTString[Either[UpstreamErrorResponse, SoapMessageStatus]](new URL(s"$destinationUrl/message"), request)
  }
}
