/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.auth.AuthFunctionality
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CorrespondenceAddressService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class CorrespondenceAddressController @Inject()(mcc: MessagesControllerComponents,
                                                correspondenceAddressService: CorrespondenceAddressService,
                                                val authConnector: DefaultAuthConnector,
                                                template: views.html.correspondenceAddress,
                                                implicit val appConfig: ApplicationConfig
                                               ) extends FrontendController(mcc)  with AuthFunctionality {

  implicit val ec: ExecutionContext = mcc.executionContext

  def editAddress(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        correspondenceAddressService.fetchCorrespondenceAddress map {
          case Some(formData) =>
            Ok(template(correspondenceAddressForm.fill(formData), mode, appConfig.atedSubsUtils.getIsoCodeTupleList, getBackLink(mode)))
          case _ =>
            Ok(template(correspondenceAddressForm, mode, appConfig.atedSubsUtils.getIsoCodeTupleList, getBackLink(mode)))
        }
      }
  }

  def submit(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        correspondenceAddressForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(template(formWithErrors,mode,appConfig.atedSubsUtils.getIsoCodeTupleList, getBackLink(mode)))),
          addressData => {
            val trimmedPostCode = appConfig.atedSubsUtils.formatPostCode(addressData.postcode)
            val trimmedAddress = addressData.copy(postcode = trimmedPostCode)
            for {
              _ <- correspondenceAddressService.saveCorrespondenceAddress(address = trimmedAddress)
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
      case Some(_) => Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
      case _ => Some(controllers.routes.RegisteredBusinessController.registeredBusinessAddress.url)
    }
  }
}
