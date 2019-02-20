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

import audit.Auditable
import config.{AtedSubscriptionFrontendAuditConnector, WSHttp}
import metrics.{Metrics, MetricsEnum}
import models._
import play.api.Mode.Mode
import play.api.{Configuration, Logger, Play}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GovernmentGatewayConnector extends ServicesConfig with RawResponseReads with Auditable {

  lazy val serviceURL = baseUrl("government-gateway")

  val enrolURI = "enrol"
  val http: CoreGet with CorePost = WSHttp

  def metrics: Metrics

  def enrol(enrolRequest: EnrolRequest)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val jsonData = Json.toJson(enrolRequest)
    val postUrl = s"""$serviceURL/$enrolURI"""
    val timerContext = metrics.startTimer(MetricsEnum.GG_CLIENT_ENROL)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      timerContext.stop()
      auditEnrolUser(enrolRequest, response)
      response.status match {
        case OK | BAD_GATEWAY =>
          metrics.incrementSuccessCounter(MetricsEnum.GG_CLIENT_ENROL)
          response
        case BAD_REQUEST =>
          metrics.incrementFailedCounter(MetricsEnum.GG_CLIENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - Bad Request Exception")
          doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new BadRequestException(response.body)
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.GG_CLIENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - status: $status")
          doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new InternalServerException(response.body)
      }
    }

  }

  private def auditEnrolUser(enrolRequest: EnrolRequest,
                             response: HttpResponse)(implicit hc: HeaderCarrier) = {

    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "enrolUser",
      detail = Map("txName" -> "enrolUser",
        "friendlyName" -> s"${enrolRequest.friendlyName}",
        "facts" -> s"${enrolRequest.knownFacts}",
        "portalId" -> s"${enrolRequest.portalId}",
        "serviceName" -> s"${enrolRequest.serviceName}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"))
  }

}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val appName: String = AppName(Play.current.configuration).appName
  override val audit: Audit = new Audit(appName, AtedSubscriptionFrontendAuditConnector)

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override def metrics = Metrics
}
