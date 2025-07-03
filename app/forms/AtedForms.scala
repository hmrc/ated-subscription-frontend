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

package forms

import models._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError}

import scala.annotation.tailrec
import scala.util.matching.Regex

object AtedForms {

  // this is with respect to play 2.3.8 but it has been changed in play 2.4; when upgrade, look into play src to change this;
  // scalastyle:off line.size.limitgit diff

  val emailRegex: Regex =
  """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r
  val addressLineLength = 35
  val PostcodeLength = 10
  val PostCodeRegex = "^[a-zA-Z]{1,2}[0-9][0-9a-zA-Z]?\\s?[0-9][a-zA-Z]{2}|BFPO\\s?[0-9]{1,10}$"
  val countryLength = 2
  val emailLength = 241
  val lengthZero = 0
  val nameLength = 35
  val phoneLength = 24
  val telephoneRegex: Regex = """^[A-Z0-9)\/(\-*#]+$""".r
  private val postcodeFormatPattern: String = "[\\s, +, ., :, _, ,, ;, =, (, ), {, }, \\[, \\], \\-, \\^, \\*]"


  val areYouAnAgentFalseConstraint: Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(false) => Valid
      case Some(true) => Invalid("ated.claim-relief.error.agent-claiming-true")
      case _ => Invalid("ated.claim-relief.error.agent-claiming")
    }
  })


  val appointAgentConstraint: Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(_) => Valid
      case _ => Invalid("ated.claim-relief.error.agent-appoint")
    }
  })

  val previousSubmittedConstraint: Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(_) => Valid
      case _ => Invalid("ated.claim-relief.error.previous-submitted")
    }
  })

  val businessAddressConstraint: Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(_) => Valid
      case _ => Invalid("ated.registered-business-address-error.correspondenceAddress")
    }
  })

  val areYouAnAgentForm = Form(mapping(
    "isAgent" -> optional(boolean).verifying(areYouAnAgentFalseConstraint)
  )(AreYouAnAgent.apply)(AreYouAnAgent.unapply)
  )

  val appointAgentForm = Form(mapping(
    "appointAgent" -> optional(boolean).verifying(appointAgentConstraint)
  )(AppointAgentForm.apply)(AppointAgentForm.unapply)
  )

  val previousSubmittedForm = Form(mapping(
    "previousSubmitted" -> optional(boolean).verifying(previousSubmittedConstraint)
  )(PreviousSubmittedForm.apply)(PreviousSubmittedForm.unapply)
  )

  val businessAddressForm = Form(mapping(
    "isCorrespondenceAddress" -> optional(boolean).verifying(businessAddressConstraint)
  )(BusinessAddress.apply)(BusinessAddress.unapply)
  )

  val correspondenceAddressForm = Form(
    mapping(
      "line_1" -> text.
        verifying("ated.error.mandatory.ated.address.line-1", x => x.trim.length > lengthZero)
        .verifying("ated.error.length.ated.address.line-1",
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength)),
      "line_2" -> text.
        verifying("ated.error.mandatory.ated.address.line-2", x => x.trim.length > lengthZero)
        .verifying("ated.error.length.ated.address.line-2",
          x => x.isEmpty || (x.nonEmpty && x.length <= addressLineLength)),
      "line_3" -> optional(text)
        .verifying("ated.error.length.ated.address.line-3",
          x => checkFieldLengthIfPopulated(x, addressLineLength)),
      "line_4" -> optional(text)
        .verifying("ated.error.length.ated.address.line-4",
          x => checkFieldLengthIfPopulated(x, addressLineLength)),
      "postcode" -> optional(text)
        .verifying("ated.error.address.postalcode.format", x => x.fold(true)(v => v.isEmpty || sanitisePostcode(v).matches(PostCodeRegex))),
      "country" -> text.
        verifying("ated.error.mandatory.ated.address.country", x => x.length > lengthZero)

    )(Address.apply)(Address.unapply))

  def checkFieldLengthIfPopulated(optionValue: Option[String], fieldLength: Int): Boolean = {
    optionValue match {
      case Some(value) => value.isEmpty || (value.nonEmpty && value.length <= fieldLength)
      case None => true
    }
  }

  private def sanitisePostcode(postcode: String): String = {
    postcode.toLowerCase().replaceAll(postcodeFormatPattern, "")
  }

  def checkForMissingSpace(optionPostcode: Option[String]): Boolean = {
    optionPostcode match {
      case Some(value) => value.contains(" ")
      case None => true
    }
  }


  val contactDetailsForm = Form(mapping(
    "firstName" -> text
      .verifying("ated.contact-details-first-name.error", x => x.trim.length > lengthZero)
      .verifying("ated.contact-details-first-name.length", x => x.isEmpty || (x.nonEmpty && x.length <= nameLength)),
    "lastName" -> text
      .verifying("ated.contact-details-last-name.error", x => x.trim.length > lengthZero)
      .verifying("ated.contact-details-last-name.length", x => x.isEmpty || (x.nonEmpty && x.length <= nameLength)),
    "telephone" -> text
      .verifying("ated.contact-details-telephone.error", x => x.trim.length > lengthZero)
      .verifying("ated.contact-details-telephone.length", x => x.isEmpty || (x.nonEmpty && x.length <= phoneLength))
      .verifying("ated.contact-details-telephone.invalidText", x => x.isEmpty || {
        val p = telephoneRegex.findFirstMatchIn(x.replaceAll(" ", "")).isDefined
        val z = x.length > phoneLength
        p || z})

  )(ContactDetails.apply)(ContactDetails.unapply))

  val emailConstraint: Constraint[Option[Boolean]] = Constraint({ model =>
    model match {
      case Some(_) => Valid
      case _ => Invalid("ated.contact-details.email.error")
    }
  })

  val contactDetailsEmailForm = Form(mapping(
    "emailConsent" -> optional(boolean).verifying(emailConstraint),
    "email" -> text
  )(ContactDetailsEmail.apply)(ContactDetailsEmail.unapply)
  )

  def validateEmail(f: Form[ContactDetailsEmail]): Form[ContactDetailsEmail] = {
    if (!f.hasErrors) {
      val emailConsent = f.data.get("emailConsent")
      val formErrors = emailConsent match {
        case Some("true") => {
          val email = f.data.getOrElse("email", "")
          if (email.isEmpty || (email.nonEmpty && email.trim.length == lengthZero)){
            Seq(FormError("email", "ated.contact-details-email.error"))
          } else if (email.length > emailLength){
            Seq(FormError("email", "ated.contact-details-email.length"))
          } else {
            val x = emailRegex.findFirstMatchIn(email).isDefined
            val y = email.length == lengthZero
            if (x || y) {
              Nil
            } else {
              Seq(FormError("email", "ated.contact-details-emailx.error"))
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
