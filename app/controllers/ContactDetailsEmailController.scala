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

class ContactDetailsEmailController @Inject()(mcc: MessagesControllerComponents,
                                              contactDetailsService: ContactDetailsService,
                                              val authConnector: DefaultAuthConnector,
                                              template: views.html.contactDetailsEmail,
                                              implicit val appConfig: ApplicationConfig
                                             ) extends FrontendController(mcc) with AuthFunctionality with WithDefaultFormBinding {

 implicit val ec: ExecutionContext = mcc.executionContext

  def view(mode: Option[String] = Some("skip")): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        contactDetailsService.fetchContactDetailsEmail map {
          case Some(formData) => Ok(template(contactDetailsEmailForm.fill(formData), mode, getBackLink(mode)))
          case _ => Ok(template(contactDetailsEmailForm, mode, getBackLink(mode)))
        }
      }
  }

  def editDetailsEmail: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        val mode = Some("edit")
        contactDetailsService.fetchContactDetailsEmail map {
          case Some(formData) => Ok(template(contactDetailsEmailForm.fill(formData), mode, getBackLink(mode)))
          case _ => Ok(template(contactDetailsEmailForm, mode, getBackLink(mode)))
        }
      }
  }

  def submit(mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        validateEmail(contactDetailsEmailForm.bindFromRequest).fold(
          formWithErrors => {
            Future.successful(BadRequest(template(formWithErrors, mode, getBackLink(mode))))
          },
          contactDetailsEmail => {
            for {
              _ <- contactDetailsService.saveContactDetailsEmail(contactDetailsEmail)
            } yield {
              Redirect(controllers.routes.ReviewBusinessDetailsController.reviewDetails)
            }
          }
        )
      }
  }

  def getBackLink(mode: Option[String]): Some[String] =
    mode match {
      case Some("edit") => Some(controllers.routes.ReviewBusinessDetailsController.reviewDetails.url)
      case _ => Some(controllers.routes.ContactDetailsController.editDetails(mode).url)
    }
}
