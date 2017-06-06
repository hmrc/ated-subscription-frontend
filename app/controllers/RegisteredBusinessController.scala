/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.auth.{AtedSubscriptionRegime, ExternalUrls}
import forms.AtedForms._
import models.BusinessAddress
import services.{CorrespondenceAddressService, RegisteredBusinessService}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import connectors.{AtedSubscriptionDataCacheConnector, DataCacheConnector}

import scala.concurrent.Future


object RegisteredBusinessController extends RegisteredBusinessController {
  val authConnector = FrontendAuthConnector
  val registeredBusinessService = RegisteredBusinessService
  val correspondenceAddressService = CorrespondenceAddressService
  val dataCacheConnector = AtedSubscriptionDataCacheConnector
}


trait RegisteredBusinessController extends FrontendController with Actions {

  val registeredBusinessService: RegisteredBusinessService
  val correspondenceAddressService: CorrespondenceAddressService
  val dataCacheConnector: DataCacheConnector

  def registeredBusinessAddress = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      for{
       businessReg <- dataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession
       address <- registeredBusinessService.getDefaultCorrespondenceAddress
      }
        yield Ok(views.html.registeredBusinessAddress(businessAddressForm.fill(businessReg.getOrElse(BusinessAddress())), address, Some(ExternalUrls.backToBusinessCustomerUrl)))
      }



  def continue = AuthorisedFor(taxRegime = AtedSubscriptionRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      businessAddressForm.bindFromRequest.fold(
        formWithErrors => {
          registeredBusinessService.getDefaultCorrespondenceAddress map { address =>
            BadRequest(views.html.registeredBusinessAddress(formWithErrors, address, Some(ExternalUrls.backToBusinessCustomerUrl)))
          }
        },
        businessAddressData => {
          dataCacheConnector.saveRegisteredBusinessDetails(businessAddressData)
          val isCorrespondenceAddress = businessAddressData.isCorrespondenceAddress.getOrElse(false)
          if (isCorrespondenceAddress) {
            registeredBusinessService.getDefaultCorrespondenceAddress flatMap { address =>
              val correspondenceAddress = correspondenceAddressService.saveCorrespondenceAddress(address)
              Future.successful(Redirect(controllers.routes.ContactDetailsController.editDetails(Some("skip"))))
            }
          } else {
            Future.successful(Redirect(controllers.routes.CorrespondenceAddressController.editAddress()))
          }
        }
      )
  }
}
