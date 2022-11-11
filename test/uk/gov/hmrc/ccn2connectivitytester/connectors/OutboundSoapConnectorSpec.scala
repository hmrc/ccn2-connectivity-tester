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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.ccn2connectivitytester.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global


class OutboundSoapConnectorSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ArgumentMatchersSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled" -> false
    )
    .build()

  trait Setup {
    val appConfigMock: AppConfig = mock[AppConfig]
    val mockDefaultHttpClient: HttpClient = mock[HttpClient]
    val mockProxiedHttpClient: ProxiedHttpClient = mock[ProxiedHttpClient]
  }

  "OutboundConnector" should {
    "use proxy when configured" in new Setup {
      when(appConfigMock.proxyRequiredForThisEnvironment).thenReturn(true)
      val underTest = new OutboundSoapConnector(appConfigMock, mockDefaultHttpClient, mockProxiedHttpClient)
      underTest.httpClient shouldBe mockProxiedHttpClient
    }

    "use default ws client when proxy is disabled" in new Setup {
      when(appConfigMock.proxyRequiredForThisEnvironment).thenReturn(false)
      val underTest = new OutboundSoapConnector(appConfigMock, mockDefaultHttpClient, mockProxiedHttpClient)
      underTest.httpClient shouldBe mockDefaultHttpClient
    }

    "use default ws client when proxy is not configured" in new Setup {
      val underTest = new OutboundSoapConnector(appConfigMock, mockDefaultHttpClient, mockProxiedHttpClient)
      underTest.httpClient shouldBe mockDefaultHttpClient
    }
  }
}
