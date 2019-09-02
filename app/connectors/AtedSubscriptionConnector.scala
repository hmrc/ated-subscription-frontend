/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import config.WSHttp
import models.{AtedSubscriptionAuthData, SubscribeSuccessResponse}
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AtedSubscriptionConnector extends AtedSubscriptionConnector {
  // $COVERAGE-OFF$
  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
  // $COVERAGE-ON$
}

trait AtedSubscriptionConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("ated-subscription")
  val subscriptionURI = "subscribe"

  val http: CoreGet with CorePost = WSHttp

  def subscribeAted(data: JsValue)(implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier): Future[SubscribeSuccessResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postURL = s"""$serviceURL$authLink/$subscriptionURI"""
    http.POST[JsValue, HttpResponse](postURL, data) map { response =>
      response.status match {
        case OK => response.json.as[SubscribeSuccessResponse]
        case BAD_REQUEST =>
          Logger.warn(s"[AtedSubscriptionConnector][subscribeAted] - Bad Request Exception ${response.body}")
          throw new BadRequestException(response.body)
        case status =>
          Logger.warn(s"[AtedSubscriptionConnector][subscribeAted] - status: $status InternalServerException ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

}
