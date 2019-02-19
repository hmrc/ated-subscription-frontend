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

package controllers

import config.FrontendAuthConnector
import connectors.{AtedSubscriptionDataCacheConnector, DataCacheConnector}
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}

import play.api.{Logger, Play}
import play.api.mvc.DiscardingCookie
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object ApplicationController extends ApplicationController {
  // $COVERAGE-OFF$
  val dataCacheConnector = AtedSubscriptionDataCacheConnector
  val authConnector = FrontendAuthConnector
  // $COVERAGE-ON$
}

trait ApplicationController extends FrontendController with RunMode with Actions {

  val dataCacheConnector: DataCacheConnector

  import play.api.Play.current

  def unauthorised() = UnauthorisedAction { implicit request =>
    Ok(views.html.unauthorised())
  }

  def cancel() = UnauthorisedAction { implicit request =>

    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"cancelRedirectUrl")
    Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
  }

  def logout = UnauthorisedAction { implicit request =>
    Redirect(ExternalUrls.logoutPath)
  }

  def keepAlive = UnauthorisedAction { implicit request =>
    Ok("OK")
  }

  def redirectToAtedStart = UnauthorisedAction {
    implicit request =>
      Redirect(ExternalUrls.atedStartPath).discardingCookies(DiscardingCookie("mdtp"))
  }

  def redirectToLogout = UnauthorisedAction {
    implicit request =>
      Redirect(ExternalUrls.logoutPath)
  }

  def unauthorisedAssistant = UnauthorisedAction {
    implicit request => Ok(views.html.unauthorisedAssistant())
  }

  def clearCache = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      dataCacheConnector.clearCache.map { x =>
        x.status match {
          case OK | NO_CONTENT =>
            Logger.info(s"session has been cleared")
            Ok
          case errorStatus => {
            Logger.error(s"session has not been cleared for ATED_SUBSCRIPTION. Status: $errorStatus, Error: ${x.body}")
            InternalServerError
          }
        }
      }
  }
}
