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
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class AtedConnector @Inject()(appConfig: ApplicationConfig,
                              http: DefaultHttpClient) extends RawResponseReads {

  val serviceURL: String = appConfig.serviceUrlAted
  val serviceUrlAtedSub: String = appConfig.serviceUrlAtedSub
  val getDetailsURI = "details"
  val retrieveSubscriptionData = "subscription-data"

  def getDetails(identifier: String, identifierType: String)
                (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    http.GET[HttpResponse](s"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType", Seq.empty, Seq.empty)
  }

  def retrieveSubscriptionData(atedRefNumber: String)
                              (implicit user: AtedSubscriptionAuthData, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"""$serviceURL$authLink/$baseURI/$retrieveSubscriptionData/$atedRefNumber"""
    http.GET[HttpResponse](getUrl, Seq.empty, Seq.empty)
  }

  def checkUsersEnrolments(safeID: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AtedUsers]] = {
    val getURL = s"""$serviceUrlAtedSub/ated/status-info/users/$safeID"""
    if(safeID == "XE0001234567892") {
      Future.successful(Some(AtedUsers(List("principalUserId1"), List("delegatedId1")))
    }
    else {
      http.GET(getURL, Seq.empty, Seq.empty) map {
        response =>
          response.status match {
            case OK => Some(response.json.as[AtedUsers])
            case status => throw new InternalServerException(s"""[awrs-frontend][checkUsersEnrolments] returned status code: $status""")
          }
      }
    }
  }

}
