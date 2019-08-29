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

import config.WSHttp
import models.{AgentEmail, ClientDisplayName, OldMandateReference}
import play.api.Mode.Mode
import play.api.mvc.Request
import play.api.{Configuration, Play}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AgentClientMandateFrontendConnector extends ServicesConfig with RawResponseReads with HeaderCarrierForPartialsConverter {

  def serviceUrl = baseUrl("agent-client-mandate-frontend")
  val emailUri = "mandate/agent/email-session"
  val displayNameUri = "mandate/agent/client-display-name-session"
  val mandateDetails = "mandate/agent/old-nonuk-mandate-from-session"
  val service = "ATED"
  val http: CoreGet

  def getAgentEmail(implicit request: Request[_]): Future[Option[AgentEmail]] = {
    val getUrl = s"$serviceUrl/$emailUri/"
    http.GET[Option[AgentEmail]](getUrl)
  }

  def getClientDisplayName(implicit request: Request[_]): Future[Option[ClientDisplayName]] = {
    val getUrl = s"$serviceUrl/$displayNameUri/"
    http.GET[Option[ClientDisplayName]](getUrl)
  }

  def getOldMandateDetails(implicit request: Request[_]): Future[Option[OldMandateReference]] = {
    val getUrl = s"$serviceUrl/$mandateDetails/"
    http.GET(getUrl) map { response => response.json.asOpt[OldMandateReference]}
  }

}

object AgentClientMandateFrontendConnector extends AgentClientMandateFrontendConnector {
  // $COVERAGE-OFF$
  val http = WSHttp
  override def crypto: (String) => String = new SessionCookieCryptoFilter(new ApplicationCrypto(Play.current.configuration.underlying)).encrypt _

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
  // $COVERAGE-ON$
}
