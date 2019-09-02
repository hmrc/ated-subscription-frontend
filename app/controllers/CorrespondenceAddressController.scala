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
import services.CorrespondenceAddressService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.AtedSubscriptionUtils

import scala.concurrent.Future

trait CorrespondenceAddressController extends FrontendController with AuthFunctionality {

  val correspondenceAddressService: CorrespondenceAddressService

  def editAddress(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        correspondenceAddressService.fetchCorrespondenceAddress map {
          case Some(formData) =>
            Ok(views.html.correspondenceAddress(correspondenceAddressForm.fill(formData), mode, AtedSubscriptionUtils.getIsoCodeTupleList, getBackLink(mode))(implicitly, implicitly, implicitly))
          case _ =>
            Ok(views.html.correspondenceAddress(correspondenceAddressForm, mode, AtedSubscriptionUtils.getIsoCodeTupleList, getBackLink(mode))(implicitly, implicitly, implicitly))
        }
      }
  }

  def submit(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        correspondenceAddressForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.correspondenceAddress(formWithErrors, mode, AtedSubscriptionUtils.getIsoCodeTupleList, getBackLink(mode)))),
          addressData => {
            val trimmedPostCode = AtedSubscriptionUtils.formatPostCode(addressData.postcode)
            val trimmedAddress = addressData.copy(postcode = trimmedPostCode)
            for {
              correspondenceAddress <- correspondenceAddressService.saveCorrespondenceAddress(address = trimmedAddress)
            } yield {
              mode match {
                case Some(edit) => Redirect(controllers.routes.ReviewBusinessDetailsController.reviewDetails)
                case _ => Redirect(controllers.routes.ContactDetailsController.editDetails())
              }
            }
          }
        )
      }
  }

  def getBackLink(mode: Option[String]): Some[String] = {
    mode match {
      case Some(edit) => Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
      case _ => Some(controllers.routes.RegisteredBusinessController.registeredBusinessAddress().url)
    }
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  val authConnector = AuthClientConnector
  val correspondenceAddressService = CorrespondenceAddressService
}
