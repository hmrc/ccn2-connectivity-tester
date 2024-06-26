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

package uk.gov.hmrc.ccn2connectivitytester.config

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {
  val addressingV2From: String                     = config.getOptional[String]("addressing.v2.from").getOrElse("partner:CCN2.Partner.XI.Customs.TAXUD/ICS_NES_V2.CONF")
  val addressingV2To: String                       = config.getOptional[String]("addressing.v2.to").getOrElse("partner:CCN2.Partner.EU.Customs.TAXUD/ICS_CR_V2.CONF")
  val appName: String                              = config.get[String]("appName")
  val outboundSoapUrl: String                      = config.get[String]("microservice.services.api-platform-outbound-soap.host")
  val notificationUrl: String                      = config.get[String]("notification.url")
  val wsdlUrlForV2                                 = config.get[String]("microservice.services.api-platform-outbound-soap.wsdl-url.v2")
  val checkInterval: Duration                      = Duration(config.getOptional[String]("check.interval").getOrElse("60 sec"))
  val checkInitialDelay: Duration                  = Duration(config.getOptional[String]("check.initial.delay").getOrElse("30 sec"))
  val checkJobLockDuration: Duration               = Duration(config.getOptional[String]("check.lock.duration").getOrElse("15 min"))
  val confirmationWaitDuration: java.time.Duration = java.time.Duration.parse(config.getOptional[String]("confirmation.wait.duration").getOrElse("PT12H"))
  val parallelism: Int                             = config.getOptional[Int]("expired.parallelism").getOrElse(5)

}
