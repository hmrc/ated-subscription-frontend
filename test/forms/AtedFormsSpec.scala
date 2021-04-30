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

package forms

import forms.AtedForms.correspondenceAddressForm
import models.Address
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class AtedFormsSpec extends PlaySpec {

  val validAddressData = Map(
    "line_1" -> "1 Ironmasters Way",
    "line_2" -> "Telford",
    "line_3" -> "Shropshire",
    "postcode" -> "TF3 4NT",
    "country" -> "GB"
  )

  val addressWithoutCountryAndPostcode = Map(
    "line_1" -> "1 Ironmasters Way",
    "line_2" -> "Telford"
  )

  "the correspondence address form" should {

    "map to an Address successfully" when {
      "the data provided is valid" in {
        correspondenceAddressForm.bind(validAddressData).value mustBe Some(Address("1 Ironmasters Way", "Telford", Some("Shropshire"), None, Some("TF3 4NT"), "GB"))
      }

      "country has been provided but no postcode" in {
        correspondenceAddressForm.bind(addressWithoutCountryAndPostcode.updated("country", "Germany"))
          .errors mustBe List()
      }

      "return a validation error" when {
        "a forward slash has been entered into an address field" in {
          for (i <- 1 to 4) {
            correspondenceAddressForm.bind(validAddressData.updated(s"line_$i", "/"))
              .errors mustBe List(FormError(s"line_$i", List(s"ated.error.address.addressline$i.format")))
          }
        }

        "country has not been provided" in {
          correspondenceAddressForm.bind(addressWithoutCountryAndPostcode.updated("postcode", "TF3 4NT"))
            .errors mustBe List(FormError("country", List("error.required")))
        }

        "postcode and country have both not been provided" in {
          val addressWithoutCountryAndPostcode = Map(
            "line_1" -> "1 Ironmasters Way",
            "line_2" -> "Telford"
          )

          correspondenceAddressForm.bind(addressWithoutCountryAndPostcode)
            .errors mustBe List(FormError("country", List("error.required")))
        }
      }
    }
  }
}
