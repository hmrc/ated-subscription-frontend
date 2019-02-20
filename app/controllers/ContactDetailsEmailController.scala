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
import controllers.auth.AtedSubscriptionRegime
import forms.AtedForms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.ContactDetailsService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait ContactDetailsEmailController extends FrontendController with Actions {

  val contactDetailsService: ContactDetailsService

  def view = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      val mode = None
      Future.successful(Ok(views.html.contactDetailsEmail(contactDetailsEmailForm, mode, getBackLink(mode))))
  }

  def editDetailsEmail = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      val mode = Some("edit")
      contactDetailsService.fetchContactDetailsEmail map {
        case Some(data) => Ok(views.html.contactDetailsEmail(contactDetailsEmailForm.fill(data), mode, getBackLink(mode)))
        case _ => Ok(views.html.contactDetailsEmail(contactDetailsEmailForm, mode, getBackLink(mode)))
      }
  }

  def submit(mode: Option[String]) = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      validateEmail(contactDetailsEmailForm.bindFromRequest).fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.contactDetailsEmail(formWithErrors, mode, getBackLink(mode))))
        },
        contactDetailsEmail => {
         for {
           contact <- contactDetailsService.saveContactDetailsEmail(contactDetailsEmail)
         } yield {
           Redirect(controllers.routes.ReviewBusinessDetailsController.reviewDetails())
         }
        }
      )
  }

  def getBackLink(mode: Option[String]) = {
    mode match {
      case Some(edit) => Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
      case _ => Some(controllers.routes.ContactDetailsController.editDetails(None).url)
    }
  }
}

object ContactDetailsEmailController extends ContactDetailsEmailController {
  val authConnector = FrontendAuthConnector
  val contactDetailsService: ContactDetailsService = ContactDetailsService
}
