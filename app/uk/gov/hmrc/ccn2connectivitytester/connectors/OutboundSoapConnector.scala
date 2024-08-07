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

package uk.gov.hmrc.ccn2connectivitytester.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.http.HttpReadsInstances.{readEitherOf, readFromJson}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.ccn2connectivitytester.models.SoapMessageStatus

@Singleton()
class OutboundSoapConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {
  lazy val destinationUri: String = appConfig.outboundSoapUrl

  def sendOutboundSoapRequest(request: String, destinationUrl: String = destinationUri): Future[Either[UpstreamErrorResponse, SoapMessageStatus]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(CONTENT_TYPE -> "application/json")

    postRequest(destinationUrl, request)
  }

  private def postRequest(destinationUrl: String, request: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, SoapMessageStatus]] = {
    httpClient.post(url"$destinationUrl/message")
      .withBody(request)
      .execute[Either[UpstreamErrorResponse, SoapMessageStatus]]
  }
}
