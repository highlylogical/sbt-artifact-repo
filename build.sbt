ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.20"

ThisBuild / organization := "com.highlylogical.oss"

enablePlugins(SbtPlugin)

lazy val root = (project in file("."))
    .settings(
        name := "sbt-artifact-repo"
    )
