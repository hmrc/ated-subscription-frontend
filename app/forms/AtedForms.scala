/*
 * Copyright 2018 HM Revenue & Customs
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

package forms

import models._
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import utils.AtedSubscriptionUtils

import scala.annotation.tailrec

object AtedForms {

  // this is with respect to play 2.3.8 but it has been changed in play 2.4; when upgrade, look into play src to change this;
  // scalastyle:off line.size.limitgit diff

  val emailRegex =
  """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r
  val addressLineLength = 35
  val postcodeLength = 9
  val countryLength = 2
  val emailLength = 241
  val lengthZero = 0
  val nameLength = 35
  val phoneLength = 24
  val telephoneRegex = """^[A-Z0-9)\/(\-*#]+$""".r
  val nameRegex = "^[a-zA-Z &`\\-\'^]{1,35}$"

  val AreYouAnAgentFalseConstraint: Constraint[AreYouAnAgent] = Constraint({ model =>
    model.isAgent.isEmpty match {
      case (false) if !model.isAgent.get => Valid
      case (false) if model.isAgent.get => Invalid("ated.claim-relief.error.agent-claiming-true", "isAgent")
      case (true) => Invalid("ated.claim-relief.error.agent-claiming", "isAgent")
    }
  })

  val areYouAnAgentForm = Form(mapping(
    "isAgent" -> optional(boolean)
  )(AreYouAnAgent.apply)(AreYouAnAgent.unapply)
    .verifying(AreYouAnAgentFalseConstraint)
  )

  val appointAgentForm = Form(mapping(
    "appointAgent" -> optional(boolean)
      .verifying(Messages("ated.claim-relief.error.agent-appoint"), result => result.isDefined)
  )(AppointAgentForm.apply)(AppointAgentForm.unapply))


  val businessAddressForm = Form(mapping(
    "isCorrespondenceAddress" -> optional(boolean)
      .verifying(Messages("ated.registered-business-address-error.correspondenceAddress"), result => result.isDefined)
  )(BusinessAddress.apply)(BusinessAddress.unapply))

  val correspondenceAddressForm = Form(
    mapping(
      "line_1" -> text.
        verifying(Messages("ated.error.mandatory", Messages("ated.address.line-1")), x => x.trim.length > lengthZero)
        .verifying(Messages("ated.error.length", Messages("ated.address.line-1"), addressLineLength),
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength)),
      "line_2" -> text.
        verifying(Messages("ated.error.mandatory", Messages("ated.address.line-2")), x => x.trim.length > lengthZero)
        .verifying(Messages("ated.error.length", Messages("ated.address.line-2"), addressLineLength),
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength)),
      "line_3" -> optional(text)
        .verifying(Messages("ated.error.length", Messages("ated.address.line-3"), addressLineLength),
          x => checkFieldLengthIfPopulated(x, addressLineLength)),
      "line_4" -> optional(text)
        .verifying(Messages("ated.error.length", Messages("ated.address.line-4"), addressLineLength),
          x => checkFieldLengthIfPopulated(x, addressLineLength)),
      "postcode" -> optional(text)
        .verifying(Messages("ated.error.address.postalcode.format", Messages("ated.address.postcode.field"), postcodeLength),
          x => checkFieldLengthIfPopulated(AtedSubscriptionUtils.formatPostCode(x), postcodeLength)),
      "country" -> text.
        verifying(Messages("ated.error.mandatory", Messages("ated.address.country")), x => x.length > lengthZero)

    )(Address.apply)(Address.unapply))

  def checkFieldLengthIfPopulated(optionValue: Option[String], fieldLength: Int): Boolean = {
    optionValue match {
      case Some(value) => value.isEmpty || (value.nonEmpty && value.length <= fieldLength)
      case None => true
    }
  }

  def checkForMissingSpace(optionPostcode: Option[String]): Boolean = {
    optionPostcode match {
      case Some(value) => value.contains(" ")
      case None => true
    }
  }


  val contactDetailsForm = Form(mapping(
    "firstName" -> text
      .verifying(Messages("ated.contact-details-first-name.error"), x => x.trim.length > lengthZero)
      .verifying(Messages("ated.contact-details-first-name.length"), x => x.trim.matches( nameRegex)),
    "lastName" -> text
      .verifying(Messages("ated.contact-details-last-name.error"), x => x.trim.length > lengthZero)
      .verifying(Messages("ated.contact-details-last-name.length"), x => x.trim.matches( nameRegex)),
    "telephone" -> text
      .verifying(Messages("ated.contact-details-telephone.error"), x => x.trim.length > lengthZero)
      .verifying(Messages("ated.contact-details-telephone.length"), x => x.isEmpty || (x.nonEmpty && x.length <= phoneLength))
      .verifying(Messages("ated.contact-details-telephone.invalidText"), x => x.isEmpty || {
        val p = telephoneRegex.findFirstMatchIn(x.replaceAll(" ", "")).exists(_ => true)
        val z = x.length > phoneLength
        p || z})

  )(ContactDetails.apply)(ContactDetails.unapply))


  val contactDetailsEmailForm = Form(mapping(
    "emailConsent" -> optional(boolean)
      .verifying(Messages("ated.contact-details.email.error"), result => result.isDefined),
    "email" -> text
  )(ContactDetailsEmail.apply)(ContactDetailsEmail.unapply))

  def validateEmail(f: Form[ContactDetailsEmail]): Form[ContactDetailsEmail] = {
    if (!f.hasErrors) {
      val emailConsent = f.data.get("emailConsent")
      val formErrors = emailConsent match {
        case Some("true") => {
          val email = f.data.get("email").getOrElse("")
          if (email.isEmpty || (email.nonEmpty && email.trim.length == lengthZero)){
            Seq(FormError("email", Messages("ated.contact-details-email.error")))
          } else if (email.length > emailLength){
            Seq(FormError("email", Messages("ated.contact-details-email.length")))
          } else {
            val x = emailRegex.findFirstMatchIn(email).exists(_ => true)
            val y = email.length == lengthZero
            if (x || y) {
              Nil
            } else {
              Seq(FormError("email", Messages("ated.contact-details-emailx.error")))
            }
          }
        }
        case _ => Nil
      }
      addErrorsToForm(f, formErrors)
    } else f
  }

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }
    y(form, formErrors)
  }

}
