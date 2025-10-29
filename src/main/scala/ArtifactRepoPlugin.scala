package com.highlylogical.oss

import sbt.io.Path
import sbt.toRepositoryName
import sbt.{AutoPlugin, Credentials, Def, Keys, MavenRepository}

import java.io.{File, FileInputStream, FilenameFilter}
import java.util.Properties
import scala.util.{Try, Success, Failure}

case class ArtifactRepoConfig(host: String, publishRepo: String, pullRepo: String, credentials: Credentials, protocol: String = "https") {
  def mavenResolver(direction: String): MavenRepository = {
    val repo = if (direction == "pull") pullRepo else publishRepo
    s"$repo-$direction" at s"$protocol://$host/$repo"
  }
}

object ArtifactRepoPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = sbt.plugins.JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = {
    val pullRepos = artifactRepos.map(_.mavenResolver("pull"))
    val publishRepo = artifactRepos.headOption.map(_.mavenResolver("publish"))
    val credentials = artifactRepos.map(_.credentials)
    
    Seq(
      Keys.credentials ++= credentials,
      Keys.resolvers ++= pullRepos,
      Keys.publishTo := publishRepo
    )
  }


  lazy val artifactRepos: Seq[ArtifactRepoConfig] = {
    val configFiles = Path.userHome.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = {
        name.endsWith(".artifactrepo")
      }
    })
    
    val results = configFiles.map(file => (file, readRepoConfig(file)))
    val (successfulConfigs, failedConfigs) = results.partition(_._2.isRight)
    
    // Log successful configuration loads
    successfulConfigs.foreach {
      case (file, Right(_)) =>
        println(s"[sbt-artifact-repo] Loaded configuration from: $file")
      case _ => // This shouldn't happen due to partition
    }
    
    // Log errors
    failedConfigs.foreach {
      case (file, Left(errorMsg)) =>
        System.err.println(s"[sbt-artifact-repo] $errorMsg")
      case _ => // This shouldn't happen due to partition
    }
    
    successfulConfigs.collect {
      case (_, Right(config)) => config
    }
  }

  import scala.collection.JavaConverters._
  
  def readRepoConfig(file: File): Either[String, ArtifactRepoConfig] = {
    Try {
      val props = new Properties()
      props.load(new FileInputStream(file))
      val keys = props.stringPropertyNames().asScala.toSet
      
      val requiredKeys = Set("realm", "host", "user", "password", "pull-repo", "publish-repo")
      val missingKeys = requiredKeys -- keys
      
      if (missingKeys.nonEmpty) {
        Left(s"Configuration file '$file' is missing required properties: ${missingKeys.mkString(", ")}")
      } else {
        Right(ArtifactRepoConfig(
          props.getProperty("host"),
          props.getProperty("publish-repo"),
          props.getProperty("pull-repo"),
          Credentials(
            props.getProperty("realm"),
            props.getProperty("host"),
            props.getProperty("user"),
            props.getProperty("password")
          ),
          props.getProperty("protocol", "https")
        ))
      }
    } match {
      case Success(result) => result
      case Failure(exception) => Left(s"Failed to read configuration file '$file': ${exception.getMessage}")
    }
  }
}

