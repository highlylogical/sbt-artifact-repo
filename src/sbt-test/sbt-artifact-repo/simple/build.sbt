version := "0.1.0"
scalaVersion := "2.12.20"

lazy val check = TaskKey[Unit]("check")
check / aggregate := true

import TestUtils._
import sbt.librarymanagement.ivy.DirectCredentials
import org.scalatest.matchers.should._


def checkImpl(projectName: String): Def.Initialize[Task[Unit]] = Def.task {
  

  val testExpectation = expectations(projectName)

  publishToShouldBe(testExpectation.publish, publishTo.value) shouldBe true
  compareCredentials(testExpectation.credentials, credentials.value) shouldBe true
  compareResolvers(testExpectation.resolvers, resolvers.value) shouldBe true
}

lazy val basicConfig = project
  .settings(
    artifactRepoConfigPath := file("./testconfigs/simpletest"),
    check := checkImpl("basic").value
  )

lazy val httpConfig = project
.settings(
  artifactRepoConfigPath := file("./testconfigs/httptest"),
  check := checkImpl("http").value
)

lazy val expectations: Map[String, Expectations] = Map(
  "basic" -> Expectations(
    Option(MavenRepository("artifacts/local-publish", "https://artifacts.acme.com/artifacts/local")),
    Seq(MavenRepository("artifacts/virtual-pull", "https://artifacts.acme.com/artifacts/virtual")),
    Seq(new DirectCredentials("Artifact Realm", "artifacts.acme.com", "repo-user", "userpw"))),
  "http" -> Expectations(
    Option(MavenRepository("artifacts/local-publish", "http://artifacts.acme.com/artifacts/local")),
    Seq(MavenRepository("artifacts/virtual-pull", "http://artifacts.acme.com/artifacts/virtual")),
    Seq(new DirectCredentials("Artifact Realm", "artifacts.acme.com", "repo-user", "userpw")))
)