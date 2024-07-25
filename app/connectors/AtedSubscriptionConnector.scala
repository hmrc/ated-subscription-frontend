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

package connectors

import config.ApplicationConfig
import javax.inject.Inject
import models.{AtedSubscriptionAuthData, SelfHealSubscriptionResponse, SubscribeSuccessResponse}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.AuthUtils
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

class AtedSubscriptionConnector @Inject()(appConfig: ApplicationConfig, http: HttpClientV2) extends Logging {

  lazy val serviceURL: String = appConfig.serviceUrlAtedSub
  val subscriptionURI: String = "subscribe"
  val checkEtmpUri: String = "regime-etmp-check"

  def subscribeAted(data: JsValue)(implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[SubscribeSuccessResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postURL = s"""$serviceURL$authLink/$subscriptionURI"""
    http.post(url"$postURL").withBody(data).execute[HttpResponse].map{response =>
      response.status match {
        case OK =>
          response.json.as[SubscribeSuccessResponse]
        case BAD_REQUEST =>
          logger.warn(s"[AtedSubscriptionConnector][subscribeAted] - Bad Request Exception ${response.body}")
          throw new BadRequestException(response.body)
        case status =>
          logger.warn(s"[AtedSubscriptionConnector][subscribeAted] - status: $status InternalServerException ${response.body}")
          throw new InternalServerException(response.body)
      }
    }
  }

  def checkEtmpBusinessPartnerExists(data: JsValue)(implicit user: AtedSubscriptionAuthData,
                                                    hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SelfHealSubscriptionResponse]] = {
    val postURL = s"""$serviceURL/$checkEtmpUri"""
    http.post(url"$postURL").withBody(data).execute[HttpResponse].map{response =>
      response.status match {
        case OK => Some(response.json.as[SelfHealSubscriptionResponse])
        case _ => None
      }
    }
  }

}
