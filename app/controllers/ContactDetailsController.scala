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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthFunctionality
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContactDetailsService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsController @Inject()(mcc: MessagesControllerComponents,
                                         contactDetailsService: ContactDetailsService,
                                         val authConnector: DefaultAuthConnector,
                                         template: views.html.contactDetails,
                                         implicit val appConfig: ApplicationConfig
                                        ) extends FrontendController(mcc) with AuthFunctionality with WithDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  def editDetails(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit user =>
        contactDetailsService.fetchContactDetails map {
          case Some(data) => Ok(template(contactDetailsForm.fill(data), mode, getBackLink(mode)))
          case _ => Ok(template(contactDetailsForm, mode, getBackLink(mode)))
        }
      }
  }

  def submit(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        contactDetailsForm.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(template(formWithErrors, mode, getBackLink(mode))))
          },
          contactDetails => {
            val telephoneWithoutSpaces = contactDetails.telephone.replaceAll(" ", "")
            contactDetailsService.saveContactDetails(contactDetails.copy(telephone = telephoneWithoutSpaces)) map {_ =>
              mode match {
                case Some(modeType) if modeType == "edit" => Redirect(controllers.routes.ReviewBusinessDetailsController.reviewDetails)
                case _ => Redirect(controllers.routes.ContactDetailsEmailController.view)
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