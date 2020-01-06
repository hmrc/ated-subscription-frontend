/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ContactDetailsService, CorrespondenceAddressService, MandateService, RegisteredBusinessService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedSubscriptionUtils

import scala.concurrent.ExecutionContext

class ReviewBusinessDetailsController @Inject()(mcc: MessagesControllerComponents,
                                                registeredBusinessService: RegisteredBusinessService,
                                                correspondenceAddressService: CorrespondenceAddressService,
                                                contactDetailsService: ContactDetailsService,
                                                mandateService: MandateService,
                                                val authConnector: DefaultAuthConnector,
                                                implicit val appConfig: ApplicationConfig
                                               ) extends FrontendController(mcc) with AuthFunctionality {
  implicit val atedSubUtils: AtedSubscriptionUtils = appConfig.atedSubsUtils
  implicit val ec: ExecutionContext = mcc.executionContext

  def reviewDetails: Action[AnyContent] = Action.async { implicit request =>
    authoriseFor { implicit auth =>
      for {
        businessDetails <- registeredBusinessService.getReviewBusinessDetails
        address <- correspondenceAddressService.fetchCorrespondenceAddress
        contactDetails <- contactDetailsService.fetchContactDetails
        contactDetailsEmail <- contactDetailsService.fetchContactDetailsEmail
        agentEmail <- mandateService.fetchEmailAddress
        clientDisplayName <- mandateService.fetchClientDisplayName
      } yield {
        Ok(views.html.reviewBusinessDetails(businessDetails,
          address.getOrElse(throw new RuntimeException("Correspondence Address not found!")),
          contactDetails.getOrElse(throw new RuntimeException("Contact Details not found!")),
          contactDetailsEmail,
          agentEmail,
          clientDisplayName,
          Some(controllers.routes.ContactDetailsEmailController.editDetailsEmail().url)
        ))
      }
    }
  }


}