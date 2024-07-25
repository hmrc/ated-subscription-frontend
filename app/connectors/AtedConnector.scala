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
import models.{AtedSubscriptionAuthData, AtedUsers}
import play.api.http.Status.OK
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.AuthUtils
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class AtedConnector @Inject()(appConfig: ApplicationConfig, http: HttpClientV2) {

  val serviceURL: String = appConfig.serviceUrlAted
  val serviceUrlAtedSub: String = appConfig.serviceUrlAtedSub
  val getDetailsURI = "details"
  val retrieveSubscriptionData = "subscription-data"

  def getDetails(identifier: String, identifierType: String)
                (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def retrieveSubscriptionData(atedRefNumber: String)
                              (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"""$serviceURL$authLink/$baseURI/$retrieveSubscriptionData/$atedRefNumber"""
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def checkUsersEnrolments(safeID: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AtedUsers]] = {
    val getURL = s"""$serviceUrlAtedSub/ated/status-info/users/$safeID"""
    http.get(url"$getURL").execute[HttpResponse].map{
      response =>
        response.status match {
          case OK => Some(response.json.as[AtedUsers])
          case status => None
        }
    }
  }
}
