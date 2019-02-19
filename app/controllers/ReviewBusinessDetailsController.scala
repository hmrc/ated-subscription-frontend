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
import services.{ContactDetailsService, CorrespondenceAddressService, MandateService, RegisteredBusinessService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait ReviewBusinessDetailsController extends FrontendController with Actions {

  val registeredBusinessService: RegisteredBusinessService
  val correspondenceAddressService: CorrespondenceAddressService
  val contactDetailsService: ContactDetailsService
  val mandateService: MandateService

  def reviewDetails = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
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

object ReviewBusinessDetailsController extends ReviewBusinessDetailsController {
  val authConnector = FrontendAuthConnector
  val registeredBusinessService = RegisteredBusinessService
  val correspondenceAddressService = CorrespondenceAddressService
  val contactDetailsService = ContactDetailsService
  val mandateService = MandateService
}
