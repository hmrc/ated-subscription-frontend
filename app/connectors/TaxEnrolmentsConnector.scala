/*
 * Copyright 2017 HM Revenue & Customs
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


import config.{AtedSubscriptionFrontendAuditConnector, WSHttp}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import metrics.{Metrics, MetricsEnum}
import audit.Auditable
import models.RequestEMACPayload
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxEnrolmentsConnector extends ServicesConfig with Auditable {
  val serviceURL = baseUrl("tax-enrolments") //

  val enrolURI = "enrol"
  val http: CoreGet with CorePost = WSHttp

  val ATED_SERVICE_NAME = "HMRC-ATED-ORG"
  val deEnrolURI = "tax-enrolments/de-enrol"
  val enrolmentUrl = s"$serviceURL/tax-enrolments"


  def metrics: Metrics

  def enrol(requestPayload: RequestEMACPayload,
            groupId: String,
            atedRefNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(MetricsEnum.API4Enrolment)

    val jsonData = Json.toJson(requestPayload)
    val enrolmentKey = s"$ATED_SERVICE_NAME~atedRefNumber~$atedRefNumber"
    println("groupId" + groupId)
    val splitGroupId = groupId.split("testGroupId-")(1)
    val postUrl = s"""$enrolmentUrl/groups/$splitGroupId/enrolments/$enrolmentKey"""
    /*val postUrl = "http://localhost:9995/tax-enrolments/groups/f0e19840-9805-41ee-b0db-1226348f1c1f/enrolments/HMRC-ATED-ORG~atedRefNumber~XY1200000100002"*/

    val timerContext = metrics.startTimer(MetricsEnum.API4Enrolment)

    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      timerContext.stop()
      auditEnrolUser(requestPayload, response)

      response.status match {
        case OK | BAD_GATEWAY =>
          metrics.incrementSuccessCounter(MetricsEnum.API4Enrolment)
          response
        case BAD_REQUEST =>
          metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - Bad Request Exception")
          doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new BadRequestException(response.body)
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
          Logger.warn(s"[ECMPGatewayConnector][enrol] - status: $status")
          doFailedAudit("enrolFailed", jsonData.toString, response.body)
          throw new InternalServerException(response.body)
      }
    }
  }


 /* def processResponse(response: HttpResponse, postUrl: String, requestPayload: RequestEMACPayload)(implicit hc: HeaderCarrier): HttpResponse = {
    response.status match {
      case OK =>
        metrics.incrementSuccessCounter(MetricsEnum.API4Enrolment)
        response
      case BAD_REQUEST =>
        metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"gg url:$postUrl, " +
          s"Bad Request Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $ATED_SERVICE_NAME")
        throw new BadRequestException(response.body)
      case NOT_FOUND =>
        metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"Not Found Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $ATED_SERVICE_NAME}")
        throw new NotFoundException(response.body)
      case SERVICE_UNAVAILABLE =>
        metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
        Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
          s"gg url:$postUrl, " +
          s"Service Unavailable Exception account Ref:${requestPayload.verifiers}, " +
          s"Service: $ATED_SERVICE_NAME}")
        throw new ServiceUnavailableException(response.body)
      case BAD_GATEWAY =>
        metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
        createWarning(postUrl, None, requestPayload.verifiers, response.body, response.status, Some("BAD_GATEWAY"))
        response
      case status =>
        metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
        createWarning(postUrl, Some(status), requestPayload.verifiers, response.body, response.status)
        throw new InternalServerException(response.body)
    }
  }*/

  private def auditEnrolUser(enrolRequest: RequestEMACPayload,
                             response: HttpResponse)(implicit hc: HeaderCarrier) = {

    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "enrolUser",
      detail = Map("txName" -> "enrolUser",
        "userId" -> s"${enrolRequest.userId}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"))
  }
}



object TaxEnrolmentsConnector extends TaxEnrolmentsConnector {
  val appName = AppName.appName
  override val metrics = Metrics
  val audit: Audit = new Audit(AppName.appName, AtedSubscriptionFrontendAuditConnector)
}
