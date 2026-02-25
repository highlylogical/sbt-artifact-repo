package com.highlylogical.oss

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import sbt.Credentials

import java.io.{File, FileWriter}
import java.nio.file.Files

class ArtifactRepoPluginSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {

  var tempDir: File = _
  
  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("artifact-repo-test").toFile
  }
  
  override def afterEach(): Unit = {
    def deleteRecursively(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles.foreach(deleteRecursively)
      }
      file.delete()
    }
    if (tempDir != null && tempDir.exists()) {
      deleteRecursively(tempDir)
    }
  }

  "ArtifactRepoConfig.mavenResolver" should "create correct pull resolver" in {
    val credentials = Credentials("Test Realm", "test.com", "user", "pass")
    val config = ArtifactRepoConfig("test.com", "maven-local", "maven-virtual", credentials, "https")
    
    val resolver = config.mavenResolver("pull")
    resolver.name should be("maven-virtual-pull")
    resolver.root should be("https://test.com/maven-virtual")
  }

  it should "create correct publish resolver" in {
    val credentials = Credentials("Test Realm", "test.com", "user", "pass")
    val config = ArtifactRepoConfig("test.com", "maven-local", "maven-virtual", credentials, "https")
    
    val resolver = config.mavenResolver("publish")
    resolver.name should be("maven-local-publish")
    resolver.root should be("https://test.com/maven-local")
  }

  it should "use custom protocol" in {
    val credentials = Credentials("Test Realm", "test.com", "user", "pass")
    val config = ArtifactRepoConfig("test.com", "maven-local", "maven-virtual", credentials, "http")
    
    val resolver = config.mavenResolver("pull")
    resolver.root should be("http://test.com/maven-virtual")
  }

  "readRepoConfig" should "successfully parse a valid config file" in {
    val configFile = new File(tempDir, "test.artifactrepo")
    val writer = new FileWriter(configFile)
    writer.write("""realm=Test Realm
                   |host=test.example.com
                   |user=testuser
                   |password=testpass
                   |pull-repo=maven-virtual
                   |publish-repo=maven-local
                   |protocol=https
                   |""".stripMargin)
    writer.close()
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    result.isRight should be(true)
    val config = result.right.get
    config.host should be("test.example.com")
    config.pullRepo should be("maven-virtual")
    config.publishRepo should be("maven-local")
    config.protocol should be("https")
  }

  it should "default to https protocol if not specified" in {
    val configFile = new File(tempDir, "test.artifactrepo")
    val writer = new FileWriter(configFile)
    writer.write("""realm=Test Realm
                   |host=test.example.com
                   |user=testuser
                   |password=testpass
                   |pull-repo=maven-virtual
                   |publish-repo=maven-local
                   |""".stripMargin)
    writer.close()
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    result.isRight should be(true)
    result.right.get.protocol should be("https")
  }

  it should "return error for missing required properties" in {
    val configFile = new File(tempDir, "test.artifactrepo")
    val writer = new FileWriter(configFile)
    writer.write("""realm=Test Realm
                   |host=test.example.com
                   |user=testuser
                   |""".stripMargin)
    writer.close()
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    result.isLeft should be(true)
    val error = result.left.get
    error should include("Not all properties present")
  }

  it should "return error for non-existent file" in {
    val configFile = new File(tempDir, "nonexistent.artifactrepo")
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    result.isLeft should be(true)
    val error = result.left.get
    error should (include("No such file") or include("nonexistent.artifactrepo"))
  }

  it should "return error for invalid file format" in {
    val configFile = new File(tempDir, "test.artifactrepo")
    val writer = new FileWriter(configFile)
    writer.write("This is not a valid properties file\n===\n")
    writer.close()
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    // Should either fail to parse or be missing required properties
    result.isLeft should be(true)
  }

  it should "handle all required properties present" in {
    val configFile = new File(tempDir, "complete.artifactrepo")
    val writer = new FileWriter(configFile)
    writer.write("""realm=Artifactory Realm
                   |host=repo.acme.com
                   |user=acme_user
                   |password=user_pat
                   |pull-repo=maven-virtual
                   |publish-repo=maven-local
                   |protocol=https
                   |extra-prop=ignored
                   |""".stripMargin)
    writer.close()
    
    val result = ArtifactRepoPlugin.readRepoConfig(configFile)
    
    result.isRight should be(true)
    val config = result.right.get
    config.host should be("repo.acme.com")
    config.pullRepo should be("maven-virtual")
    config.publishRepo should be("maven-local")
  }
}
