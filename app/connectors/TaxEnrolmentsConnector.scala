/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import metrics.{Metrics, MetricsEnum}
import audit.Auditable
import models.RequestEMACPayload
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import utils.AtedSubscriptionUtils._
import utils.GovernmentGatewayConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaxEnrolmentsConnector extends ServicesConfig with Auditable {
  val serviceURL = baseUrl("tax-enrolments")

  val enrolURI = "enrol"
  val http: CoreGet with CorePost = WSHttp

  val enrolmentUrl = s"$serviceURL/tax-enrolments"

  def metrics: Metrics

  def enrol(requestPayload: RequestEMACPayload,
            groupId: String,
            atedRefNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(MetricsEnum.API4Enrolment)

    val jsonData = Json.toJson(requestPayload)
    val enrolmentKey = s"${GovernmentGatewayConstants.ATED_SERVICE_NAME}~ATEDRefNumber~$atedRefNumber"

    val postUrl = s"""$enrolmentUrl/groups/$groupId/enrolments/$enrolmentKey"""

    val timerContext = metrics.startTimer(MetricsEnum.API4Enrolment)

    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      timerContext.stop()
      auditEnrolUser(postUrl, requestPayload, response)

      Logger.debug(s"PostUrl::$postUrl ---- requestBody:: $jsonData --- responseBody::${response.body} --- responseStatus:: ${response.status}")

      response.status match {
        case CREATED =>
          metrics.incrementSuccessCounter(MetricsEnum.API4Enrolment)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
          Logger.warn(s"[TaxEnrolmentsConnector][enrol] - status: $status")
          doFailedAudit("enrolFailed", jsonData.toString, response.body)
          response
      }
    }
  }

  private def auditEnrolUser(postUrl: String,
                             enrolRequest: RequestEMACPayload,
                             response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case CREATED => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "emacEnrolCall",
      detail = Map("txName" -> "emacEnrolCall",
        "userId" -> s"${enrolRequest.userId}",
        "serviceName" -> s"${GovernmentGatewayConstants.ATED_SERVICE_NAME}",
        "postUrl" -> s"$postUrl",
        "requestBody" -> s"${Json.prettyPrint(Json.toJson(enrolRequest))}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"))
  }
}

object TaxEnrolmentsConnector extends TaxEnrolmentsConnector {
  // $COVERAGE-OFF$
  val appName = AppName.appName
  override val metrics = Metrics
  val audit: Audit = new Audit(AppName.appName, AtedSubscriptionFrontendAuditConnector)
  // $COVERAGE-ON$
}
