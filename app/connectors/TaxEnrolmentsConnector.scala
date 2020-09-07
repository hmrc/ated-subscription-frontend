/*
 * Copyright 2020 HM Revenue & Customs
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
import models.RequestEMACPayload
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnector @Inject()(appConfig: ApplicationConfig,
                                       auditable: Auditable,
                                       http: DefaultHttpClient,
                                       metrics: Metrics
                                      ) extends  Logging {
  lazy val serviceURL: String = appConfig.serviceUrlTaxEnrol
  val enrolURI: String = "enrol"
  val enrolmentUrl: String = s"$serviceURL/tax-enrolments"

  def enrol(requestPayload: RequestEMACPayload,
            groupId: String,
            atedRefNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val jsonData = Json.toJson(requestPayload)
    val enrolmentKey = s"HMRC-ATED-ORG~ATEDRefNumber~$atedRefNumber"
    val postUrl = s"""$enrolmentUrl/groups/$groupId/enrolments/$enrolmentKey"""
    val timerContext = metrics.startTimer(MetricsEnum.API4Enrolment)

    http.POST[JsValue, HttpResponse](postUrl, jsonData)(implicitly, HttpReads.Implicits.readRaw,implicitly,implicitly) map { response =>
      timerContext.stop()
      auditEnrolUser(postUrl, requestPayload, response)
      logger.debug(s"PostUrl::$postUrl ---- requestBody:: $jsonData --- responseBody::${response.body} --- responseStatus:: ${response.status}")

      response.status match {
        case CREATED =>
          metrics.incrementSuccessCounter(MetricsEnum.API4Enrolment)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.API4Enrolment)
          logger.warn(s"[TaxEnrolmentsConnector][enrol] - status: $status")
          auditable.doFailedAudit("enrolFailed", jsonData.toString, response.body)
          response
      }
    }
  } recover handleErrorResponse

  private def handleErrorResponse: PartialFunction[Throwable, HttpResponse] = {
    case ex: ConflictException => HttpResponse.apply(CONFLICT, ex.getMessage)
    case ex: BadRequestException => throw new RuntimeException("[TaxEnrolmentsConnector][handleErrorResponse]" +
      s" - ES8 Failed with status: ${ex.responseCode} message: ${ex.message}")
    case ex: UpstreamErrorResponse => HttpResponse.apply(ex.statusCode, ex.getMessage)
    case ex: Exception => HttpResponse.apply(INTERNAL_SERVER_ERROR, ex.getMessage)
  }

  private def auditEnrolUser(postUrl: String,
                             enrolRequest: RequestEMACPayload,
                             response: HttpResponse)(implicit hc: HeaderCarrier): Unit = {
    val eventType = response.status match {
      case CREATED => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    auditable.sendDataEvent(transactionName = "emacEnrolCall",
      detail = Map("txName" -> "emacEnrolCall",
        "userId" -> s"${enrolRequest.userId}",
        "serviceName" -> "HMRC-ATED-ORG",
        "postUrl" -> s"$postUrl",
        "requestBody" -> s"${Json.prettyPrint(Json.toJson(enrolRequest))}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"))
  }
}
