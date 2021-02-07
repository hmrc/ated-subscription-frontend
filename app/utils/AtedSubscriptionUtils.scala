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

package utils

import java.util.Properties

import javax.inject.Inject
import play.api.Environment

import scala.collection.JavaConverters
import scala.io.Source

class AtedSubscriptionUtilsImpl @Inject()(val environment: Environment) extends AtedSubscriptionUtils

trait AtedSubscriptionUtils {

  def environment: Environment

  private[utils] val readJson = {
    environment.resourceAsStream("country-code.properties") match {
      case Some(inputStream) => Source.fromInputStream(inputStream, "UTF-8").bufferedReader()
      case _                 => throw new Exception("Country codes file not found")
    }
  }

  lazy val p = new Properties
  p.load(readJson)

  def getIsoCodeTupleList: List[(String, String)] = {
    JavaConverters.propertiesAsScalaMapConverter(p).asScala.toList.sortBy(_._2)
  }

  def getSelectedCountry(isoCode: String): String = {
    def trimCountry(selectedCountry: String) = {
      val position = selectedCountry.indexOf(":")
      if (position > 0) selectedCountry.substring(0, position).trim
      else selectedCountry
    }

    def getCountry(isoCode: String): Option[String] = {
      val country = Option(p.getProperty(isoCode.toUpperCase))
      country.map { selectedCountry => trimCountry(selectedCountry) }
    }

    getCountry(isoCode.toUpperCase).fold(isoCode) { x => x }
  }

  def formatPostCode(postCode: Option[String]): Option[String] = {
    postCode.map { foundPostCode =>
      val trimmedPostcode = foundPostCode.replaceAll(" ", "").toUpperCase()
      val postCodeSplit = trimmedPostcode splitAt (trimmedPostcode.length - 3)
      postCodeSplit._1 + " " + postCodeSplit._2
    }
  }

  def validateGroupId(str: String): String = if(str.trim.length != 36) {
    if(str.contains("testGroupId-")) str.replace("testGroupId-", "")
    else throw new RuntimeException("Invalid groupId from auth")
  } else str.trim

}
