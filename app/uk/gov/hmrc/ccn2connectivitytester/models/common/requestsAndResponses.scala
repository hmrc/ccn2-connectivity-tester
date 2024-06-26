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

package uk.gov.hmrc.ccn2connectivitytester.models.common

import java.time.Instant
import javax.inject.Inject

import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig

sealed trait UpdateResult

case object MessageIdNotFoundResult extends UpdateResult

case object UpdateSuccessResult extends UpdateResult

sealed trait SendResult

case object SuccessResult extends SendResult

case object FailResult extends SendResult

case object RetryingResult extends SendResult

class Requests @Inject() (appConfig: AppConfig) {

  def getV2Request: String = {
    val messageId = s"ISALIVE-${Instant.now().getEpochSecond}-V2"
    s"""
       |{"wsdlUrl":"${appConfig.wsdlUrlForV2}",
       | "wsdlOperation":"IsAlive", "messageBody":"", "confirmationOfDelivery": true,
       | "addressing": {
       |   "to":"${appConfig.addressingV2To}",
       |   "from":"${appConfig.addressingV2From}",
       |   "replyTo":"${appConfig.addressingV2From}",
       |   "faultTo":"${appConfig.addressingV2From}",
       |   "messageId":"$messageId"
       | },
       |  "confirmationOfDelivery": true,
       |  "notificationUrl": "${appConfig.notificationUrl}"
       |}
    """.stripMargin
  }
}
