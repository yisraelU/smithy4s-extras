ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"
Global / onChangedBuildSource := ReloadOnSourceChanges
lazy val root = (project in file("."))
  .aggregate(curl)


lazy val curl = (project in file("modules/curl"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    name := "smithy4s-extras",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.39",
      "org.http4s" %% "http4s-ember-client" % "0.23.39",
      "org.scalameta" %% "munit" % "1.2.2" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )