
import TestPhases._
import sbt.Keys._
import sbt.{Def, inConfig, _}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ated-subscription-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val playSettings: Seq[Setting[_]] = Seq.empty
val silencerVersion = "1.7.12"

lazy val scoverageSettings: Seq[Def.Setting[_ >: String with Double with Boolean]] = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;views.html.*;views.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin): _*)
  .settings(playSettings: _*)
  .settings(majorVersion := 2 )
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalacOptions += "-Ywarn-unused:-explicits,-implicits")
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    Test / parallelExecution := false,
    Test / fork := false
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false,
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
    )
   // ***************
  )
  .settings(
    resolvers += Resolver.jcenterRepo
  )
  .disablePlugins(JUnitXmlReportPlugin)