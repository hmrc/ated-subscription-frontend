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
import config.ApplicationConfig
import javax.inject.Inject
import metrics.{Metrics, MetricsEnum}
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class GovernmentGatewayConnector @Inject()(appConfig: ApplicationConfig,
                                           auditable: Auditable,
                                           http: DefaultHttpClient,
                                           metrics: Metrics
                                          ) extends RawResponseReads {
  lazy val serviceURL: String = appConfig.serviceUrlGG
  val enrolURI = "enrol"

  def enrol(enrolRequest: EnrolRequest)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
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
          auditable.doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new BadRequestException(response.body)
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.GG_CLIENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - status: $status")
          auditable.doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new InternalServerException(response.body)
      }
    }
  }

  private def auditEnrolUser(enrolRequest: EnrolRequest,
                             response: HttpResponse)(implicit hc: HeaderCarrier): Unit = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    auditable.sendDataEvent(transactionName = "enrolUser",
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