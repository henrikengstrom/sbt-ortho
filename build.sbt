import scalariform.formatter.preferences._

sbtPlugin := true

lazy val commonSettings = Seq(
  version in ThisBuild := "0.2-SNAPSHOT",
  organization in ThisBuild := "org.h3nk3",
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4"
)

lazy val root = (project in file(".")).
  settings(commonSettings).
  settings(
    sbtPlugin := true,
    name := "sbt-ortho",
    description := "sbt plugin for spell checking and conversion of English styles of writing",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := false,
    bintrayRepository in bintray := "sbt-plugins",
    bintrayOrganization in bintray := None
  )

scalariformSettings
ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

