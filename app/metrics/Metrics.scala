/*
 * Copyright 2022 HM Revenue & Customs
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

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import com.codahale.metrics.Timer.Context
import metrics.MetricsEnum.MetricsEnum

class Metrics {

  val registry = new MetricRegistry

  val timers: Map[metrics.MetricsEnum.Value, Timer] = Map(
      MetricsEnum.GG_CLIENT_ENROL -> registry.timer("gg-enrol-client-ated-response-timer"),
      MetricsEnum.API4Enrolment -> registry.timer("api4-enrolment-response-timer"),
      MetricsEnum.API10DeEnrolment -> registry.timer("api10-de-enrolment-response-timer")
  )

  val successCounters: Map[metrics.MetricsEnum.Value, Counter] = Map(
    MetricsEnum.GG_CLIENT_ENROL -> registry.counter("gg-enrol-client-ated-success-counter"),
    MetricsEnum.API4Enrolment -> registry.counter("api4-enrolment-success"),
    MetricsEnum.API10DeEnrolment -> registry.counter("api10-de-enrolment-success")
  )

  val failedCounters: Map[metrics.MetricsEnum.Value, Counter] = Map(
    MetricsEnum.GG_CLIENT_ENROL -> registry.counter("gg-enrol-client-ated-failed-counter"),
    MetricsEnum.API4Enrolment -> registry.counter("api4-enrolment-failed"),
    MetricsEnum.API10DeEnrolment -> registry.counter("api10-de-enrolment-failed")
  )

  def startTimer(api: MetricsEnum): Context = timers(api).time()

  def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()

  def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()
}
