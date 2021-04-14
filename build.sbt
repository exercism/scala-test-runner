import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.exercism"
ThisBuild / organizationName := "exercism"

lazy val root = (project in file("."))
  .settings(
    name := "TestRunner",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.json" % "json" % "20201115"
  )
