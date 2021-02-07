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
import connectors.AtedSubscriptionDataCacheConnector
import controllers.auth.AuthFunctionality
import forms.AtedForms._
import javax.inject.Inject
import models.{Address, AtedSubscriptionAuthData, BusinessAddress, BusinessCustomerDetails}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{CorrespondenceAddressService, EtmpCheckService, RegisteredBusinessService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedSubscriptionUtils

import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessController @Inject()(mcc: MessagesControllerComponents,
                                             registeredBusinessService: RegisteredBusinessService,
                                             correspondenceAddressService: CorrespondenceAddressService,
                                             dataCacheConnector: AtedSubscriptionDataCacheConnector,
                                             etmpCheckService: EtmpCheckService,
                                             val authConnector: DefaultAuthConnector,
                                             template: views.html.registeredBusinessAddress,
                                             implicit val appConfig: ApplicationConfig
                                            ) extends FrontendController(mcc) with AuthFunctionality {

  implicit val atedSubUtils: AtedSubscriptionUtils = appConfig.atedSubsUtils
  implicit val ec: ExecutionContext = mcc.executionContext

  def registeredBusinessAddress: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        registeredBusinessService.getBusinessCustomerDetails flatMap { customerDetails =>
          dataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession flatMap { businessReg =>
            registeredBusinessService.getDefaultCorrespondenceAddress(Some(customerDetails.businessAddress)) flatMap { address =>
              validateAndRedirect(customerDetails, businessReg, address)
            }
          }
        }
      }
  }

  private def validateAndRedirect(bcDetails: BusinessCustomerDetails, businessReg: Option[BusinessAddress], address: Address)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext, auth: AtedSubscriptionAuthData,
                                  req: Request[AnyContent], messages: Messages): Future[Result] = {
    val standardView =
      Future.successful(Ok(template(businessAddressForm.fill(
        businessReg.getOrElse(BusinessAddress())), address, Some(appConfig.backToBusinessCustomerUrl))
      ))

    etmpCheckService.validateBusinessDetails(bcDetails) flatMap { etmpRegistered =>
      if (etmpRegistered) {
        authoriseFor { newAuthDetails =>
          if (newAuthDetails.enrolments.getEnrolment("HMRC-ATED-ORG").isDefined) {
            Future.successful(Redirect(appConfig.atedStartPath))
          } else {
            standardView
          }
        }
      } else {
        standardView
      }
    }
  }

  def continue: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        businessAddressForm.bindFromRequest.fold(
          formWithErrors => {
            registeredBusinessService.getDefaultCorrespondenceAddress().map { address =>
              BadRequest(template(formWithErrors, address, Some(appConfig.backToBusinessCustomerUrl)))
            }
          },
          businessAddressData => {
            dataCacheConnector.saveRegisteredBusinessDetails(businessAddressData)
            val isCorrespondenceAddress = businessAddressData.isCorrespondenceAddress.getOrElse(false)
            if (isCorrespondenceAddress) {
              for {
                address <- registeredBusinessService.getDefaultCorrespondenceAddress()
                _ <- correspondenceAddressService.saveCorrespondenceAddress(address)
              } yield {
                Redirect(controllers.routes.ContactDetailsController.editDetails(Some("skip")))
              }
            } else {
              Future.successful(Redirect(controllers.routes.CorrespondenceAddressController.editAddress()))
            }
          }
        )
      }
  }
}
