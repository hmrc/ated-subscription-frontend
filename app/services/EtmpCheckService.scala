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

package services

import config.ApplicationConfig
import connectors.{AtedSubscriptionConnector, TaxEnrolmentsConnector}
import javax.inject.Inject
import models.{AtedSubscriptionAuthData, BusinessCustomerDetails}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EtmpCheckService @Inject()(atedSubscriptionConnector: AtedSubscriptionConnector,
                                 taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                 registerUserService: RegisterUserService,
                                 appConfig: ApplicationConfig) extends Logging {

  def validateBusinessDetails(busCusDetails: BusinessCustomerDetails)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext, authData: AtedSubscriptionAuthData): Future[Boolean] = {

    logger.info("[CheckEtmpService][validateBusinessDetails] Validating business details for self-heal")

    atedSubscriptionConnector.checkEtmpBusinessPartnerExists(Json.toJson(busCusDetails)) flatMap {
      case Some(response) =>
        (authData.groupIdentifier, authData.credId) match {
          case (Some(groupId), Some(credId)) =>
            val requestPayload = registerUserService.createEnrolmentRequest(
              busCusDetails.businessType,
              credId,
              busCusDetails.utr,
              busCusDetails.businessAddress.postcode.map(_.replaceAll("\\s+", "")),
              busCusDetails.safeId
            )
            val validatedGroupId = appConfig.atedSubsUtils.validateGroupId(groupId)

            logger.info(s"[EtmpCheckService][validateBusinessDetails] - attempting ATED enrolment")

            taxEnrolmentsConnector.enrol(requestPayload, validatedGroupId, response.regimeRefNumber) map { resp =>
              resp.status match {
                case CREATED =>
                  logger.info("[EtmpCheckService][validateBusinessDetails] ES8 success")
                  true
                case _ =>
                  logger.info("[EtmpCheckService][validateBusinessDetails] ES8 failure")
                  false
              }
            }
          case _ =>
            logger.info("[EtmpCheckService][validateBusinessDetails] No group identifier or credId for user")
            Future.successful(false)
        }
      case None =>
        logger.info("[EtmpCheckService][validateBusinessDetails] Could not perform ES6")
        Future.successful(false)
    }
  }
}
