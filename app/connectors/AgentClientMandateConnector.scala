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
import models.NonUKClientDto
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AgentClientMandateConnector extends AgentClientMandateConnector {
  // $COVERAGE-OFF$
  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
  // $COVERAGE-ON$
}

trait AgentClientMandateConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("agent-client-mandate")
  val createMandateURI = "mandate/non-uk"
  val updateMandateURI = "mandate/non-uk/update"


  val http: CoreGet with CorePost = WSHttp

  def createMandateForNonUK(dto: NonUKClientDto)(implicit user: AuthContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val data = Json.toJson(dto)
    val authLink = AuthUtils.agentLink
    val postURL = s"""$serviceURL$authLink/$createMandateURI"""
    http.POST[JsValue, HttpResponse](postURL, data) map { response =>
      response.status match {
        case CREATED => response
        case status =>
          Logger.warn(s"[AgentClientMandateConnector][createMandateForNonUK]- Exception occured - status:: $status response:: ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

  def updateMandateForNonUK(dto: NonUKClientDto)(implicit user: AuthContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val data = Json.toJson(dto)
    val authLink = AuthUtils.agentLink
    val postURL = s"""$serviceURL$authLink/$updateMandateURI"""
    http.POST[JsValue, HttpResponse](postURL, data) map { response =>
      response.status match {
        case CREATED => response
        case status =>
          Logger.warn(s"[AgentClientMandateConnector][updateMandateForNonUK]- Exception occured - status:: $status response:: ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

}
