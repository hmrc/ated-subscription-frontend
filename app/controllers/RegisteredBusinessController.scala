/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.{AtedConnector, AtedSubscriptionDataCacheConnector, BusinessCustomerFrontendConnector}
import controllers.auth.AuthFunctionality
import forms.AtedForms._

import javax.inject.Inject
import models.{Address, AtedSubscriptionAuthData, AtedUsers, BusinessAddress, BusinessCustomerDetails}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{CorrespondenceAddressService, EtmpCheckService, RegisteredBusinessService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedSubscriptionUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import scala.concurrent.{ExecutionContext, Future}

class RegisteredBusinessController @Inject()(mcc: MessagesControllerComponents,
                                             registeredBusinessService: RegisteredBusinessService,
                                             correspondenceAddressService: CorrespondenceAddressService,
                                             dataCacheConnector: AtedSubscriptionDataCacheConnector,
                                             businessCustomerFEConnector: BusinessCustomerFrontendConnector,
                                             etmpCheckService: EtmpCheckService,
                                             atedConnector: AtedConnector,
                                             val authConnector: DefaultAuthConnector,
                                             template: views.html.registeredBusinessAddress,
                                             templateAlreadyRegistered: views.html.registeredWithDifferentGG,
                                             implicit val appConfig: ApplicationConfig
                                            ) extends FrontendController(mcc) with AuthFunctionality with WithUnsafeDefaultFormBinding {

  implicit val atedSubUtils: AtedSubscriptionUtils = appConfig.atedSubsUtils
  implicit val ec: ExecutionContext = mcc.executionContext

  def registeredBusinessAddress: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        registeredBusinessService.getBusinessCustomerDetails flatMap { customerDetails =>
          atedConnector.checkUsersEnrolments(customerDetails.safeId) flatMap { atedUsers =>
            dataCacheConnector.fetchAndGetRegisteredBusinessDetailsForSession flatMap { businessReg =>
              registeredBusinessService.getDefaultCorrespondenceAddress(Some(customerDetails.businessAddress)) flatMap { address =>
                validateAndRedirect(customerDetails, businessReg, address, atedUsers)
              }
            }
          }
        }
      }
  }

  private def validateAndRedirect(bcDetails: BusinessCustomerDetails, businessReg: Option[BusinessAddress], address: Address, atedUsers: Option[AtedUsers])
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext,
                                  auth: AtedSubscriptionAuthData,
                                  req: Request[AnyContent], messages: Messages): Future[Result] = {
    val backLinkUrlFromAcm: Option[String] = req.queryString.get("backLinkUrl").map(s => s.headOption.getOrElse(""))
    val standardView = {
      if (backLinkUrlFromAcm.getOrElse("") contains "/mandate/agent/search-previous/nrl") {
      Future.successful(Ok(template(businessAddressForm.fill(
        businessReg.getOrElse(BusinessAddress())), address, Some(appConfig.backToSearchPreviousNrlUrl))
      ))
    }
      else {
        businessCustomerFEConnector.getBackLinkStatus.flatMap(response =>
          (response.status: @unchecked) match {
            case OK => Future.successful(Ok(template(businessAddressForm.fill(
              businessReg.getOrElse(BusinessAddress())), address, Some(appConfig.backToBusinessCustomerUrl))
            ))
            case _ =>
              //Ramesh TO DO :: Exception handling
              //logger
              //logger.error ("dsnckdcnkdwc")
              Future.successful(Ok(templateAlreadyRegistered(bcDetails.businessName)))
          })
      }}

    atedUsers match {
      case Some(users) =>
        if(users.principalGroupIds == Nil) {
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
        else {
          Future.successful(Ok(templateAlreadyRegistered(bcDetails.businessName)))
        }
      case _ => standardView
    }
  }

  def continue: Action[AnyContent] = Action.async {
    implicit request =>
      authoriseFor { implicit data =>
        businessAddressForm.bindFromRequest().fold(
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
