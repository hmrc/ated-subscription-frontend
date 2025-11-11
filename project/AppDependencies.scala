
import play.sbt.PlayImport.*
import sbt.*

private object AppDependencies {

  val bootstrapVersion = "10.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30"  % "12.20.0",
    "uk.gov.hmrc"   %% "play-partials-play-30"       % "10.2.0",
    "uk.gov.hmrc"   %% "domain-play-30"              % "11.0.0",
    "uk.gov.hmrc"   %% "http-caching-client-play-30" % "12.2.0",
    "commons-codec" %  "commons-codec"               % "1.20.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-test-play-30"% bootstrapVersion,
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq()
  def apply(): Seq[ModuleID] = compile ++ test
}
