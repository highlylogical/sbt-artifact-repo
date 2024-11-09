import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / scalaVersion := "2.12.20"

ThisBuild / organization := "com.highlylogical.oss"

ThisBuild / licenses := List(License.MIT)

ThisBuild / developers := List(
    Developer("michaelpigg", "Michael Pigg", "mikepigg@highlylogical.com", url("https://github.com/michaelpigg")
  )
)

enablePlugins(SbtPlugin)

lazy val root = (project in file("."))
    .settings(
        name := "sbt-artifact-repo"
    )
