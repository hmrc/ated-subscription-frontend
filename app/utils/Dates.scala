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

package utils

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId, ZonedDateTime}

object Dates {

  private[utils] val dateFormat =
    DateTimeFormatter.ofPattern("d MMMM y").withZone(ZoneId.of("UTC"))
  private[utils] val dateFormatAbbrMonth =
    DateTimeFormatter.ofPattern("d MMM y").withZone(ZoneId.of("UTC"))
  private[utils] val shortDateFormat =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))
  private[utils] val easyReadingDateFormat =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy").withZone(ZoneId.of("UTC"))

  def formatDate(date: ZonedDateTime) = dateFormat.format(date)

  def formatDateAbbrMonth(date: LocalDate) = dateFormatAbbrMonth.format(date)

  def formatDate(date: Option[LocalDate], default: String) = date match {
    case Some(d) => dateFormat.format(d)
    case None    => default
  }

  def formatDateTime(date: ZonedDateTime) = dateFormat.format(date)

  def shortDate(date: LocalDate) = shortDateFormat.format(date)

  def formatDays(days: Int) = s"$days day${if (days > 1) "s" else ""}"

}
