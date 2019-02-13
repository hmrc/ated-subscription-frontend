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

package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.MetricsRegistry
import metrics.MetricsEnum.MetricsEnum
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait Metrics {

  def startTimer(api: MetricsEnum): Timer.Context
  def incrementSuccessCounter(api: MetricsEnum): Unit
  def incrementFailedCounter(api: MetricsEnum): Unit

}

object Metrics extends Metrics with MicroserviceMetrics{

  val registry = metrics.defaultRegistry
  val timers = Map(
      MetricsEnum.GG_CLIENT_ENROL -> registry.timer("gg-enrol-client-ated-response-timer"),
      MetricsEnum.API4Enrolment -> registry.timer("api4-enrolment-response-timer"),
      MetricsEnum.API10DeEnrolment -> metrics.defaultRegistry.timer("api10-de-enrolment-response-timer")
  )

  val successCounters = Map(
    MetricsEnum.GG_CLIENT_ENROL -> registry.counter("gg-enrol-client-ated-success-counter"),
    MetricsEnum.API4Enrolment -> metrics.defaultRegistry.counter("api4-enrolment-success"),
    MetricsEnum.API10DeEnrolment -> metrics.defaultRegistry.counter("api10-de-enrolment-success")
  )

  val failedCounters = Map(
    MetricsEnum.GG_CLIENT_ENROL -> registry.counter("gg-enrol-client-ated-failed-counter"),
    MetricsEnum.API4Enrolment -> metrics.defaultRegistry.counter("api4-enrolment-failed"),
    MetricsEnum.API10DeEnrolment -> metrics.defaultRegistry.counter("api10-de-enrolment-failed")
  )

  override def startTimer(api: MetricsEnum): Context = timers(api).time()

  override def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()

}
