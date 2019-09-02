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

import config.AuthClientConnector
import controllers.auth.AuthFunctionality
import forms.AtedForms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.ContactDetailsService
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait ContactDetailsController extends FrontendController with AuthFunctionality {

  val contactDetailsService: ContactDetailsService

  def editDetails(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit user =>
        contactDetailsService.fetchContactDetails map {
          case Some(data) => Ok(views.html.contactDetails(contactDetailsForm.fill(data), mode, getBackLink(mode)))
          case _ => Ok(views.html.contactDetails(contactDetailsForm, mode, getBackLink(mode)))
        }
      }
  }

  def submit(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        contactDetailsForm.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.contactDetails(formWithErrors, mode, getBackLink(mode))))
          },
          contactDetails => {
            val telephoneWithoutSpaces = contactDetails.telephone.replaceAll(" ", "")
            contactDetailsService.saveContactDetails(contactDetails.copy(telephone = telephoneWithoutSpaces)) map {_ =>
              mode match {
                case Some(modeType) if (modeType == "edit") => Redirect(controllers.routes.ReviewBusinessDetailsController.reviewDetails)
                case _ => Redirect(controllers.routes.ContactDetailsEmailController.view())
              }
            }
          }
        )
      }
  }

  def getBackLink(mode: Option[String]): Some[String] = {
    mode match {
      case Some(modeType) if modeType == "edit" => Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
      case Some(modeType) if modeType == "skip" => Some(controllers.routes.RegisteredBusinessController.registeredBusinessAddress.url)
      case _ => Some(controllers.routes.CorrespondenceAddressController.editAddress().url)
    }
  }
}

object ContactDetailsController extends ContactDetailsController {
  val authConnector = AuthClientConnector
  val contactDetailsService: ContactDetailsService = ContactDetailsService
}
