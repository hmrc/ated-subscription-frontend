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

import config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import utils.AuthUtils

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse }

object AtedConnector extends AtedConnector {
  val serviceURL = baseUrl("ated")
  val http = WSHttp
}

trait AtedConnector extends ServicesConfig with RawResponseReads {

  def serviceURL: String
  val getDetailsURI = "details"
  val retrieveSubscriptionData = "subscription-data"

  def http: HttpGet with HttpPost

  def getDetails(identifier: String, identifierType: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    http.GET[HttpResponse](s"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType")
  }

  def retrieveSubscriptionData(atedRefNumber: String)(implicit user: AuthContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"""$serviceURL$authLink/$baseURI/$retrieveSubscriptionData/$atedRefNumber"""
    http.GET[HttpResponse](getUrl)
  }

}
