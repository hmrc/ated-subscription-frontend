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

package connectors

import config.ApplicationConfig
import javax.inject.Inject
import models.{AtedSubscriptionAuthData, NonUKClientDto}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class AgentClientMandateConnector @Inject()(appConfig: ApplicationConfig,
                                            http: DefaultHttpClient
                                           ) extends RawResponseReads with Logging {

  lazy val serviceURL: String = appConfig.serviceUrlACM
  val createMandateURI = "mandate/non-uk"
  val updateMandateURI = "mandate/non-uk/update"

  def createMandateForNonUK(dto: NonUKClientDto)
                           (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val data = Json.toJson(dto)
    val authLink = AuthUtils.agentLink
    val postURL = s"""$serviceURL$authLink/$createMandateURI"""
    http.POST[JsValue, HttpResponse](postURL, data, Seq.empty) map { response =>
      response.status match {
        case CREATED => response
        case status =>
          logger.warn(s"[AgentClientMandateConnector][createMandateForNonUK]- Exception occured - status:: $status response:: ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

  def updateMandateForNonUK(dto: NonUKClientDto)
                           (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val data = Json.toJson(dto)
    val authLink = AuthUtils.agentLink
    val postURL = s"""$serviceURL$authLink/$updateMandateURI"""
    http.POST[JsValue, HttpResponse](postURL, data, Seq.empty) map { response =>
      response.status match {
        case CREATED => response
        case status =>
          logger.warn(s"[AgentClientMandateConnector][updateMandateForNonUK]- Exception occurred - status:: $status response:: ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

}
