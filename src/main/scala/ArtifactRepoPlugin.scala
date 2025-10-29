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
  
  // Enable debug logging with -Dsbt.artifact.repo.debug=true
  private val debugEnabled: Boolean = sys.props.get("sbt.artifact.repo.debug").contains("true")
  
  private def debug(msg: => String): Unit = {
    if (debugEnabled) {
      println(s"[sbt-artifact-repo][DEBUG] $msg")
    }
  }

  override def projectSettings: Seq[Def.Setting[_]] = {
    debug("Initializing artifact repository plugin")
    val pullRepos = artifactRepos.map(_.mavenResolver("pull"))
    val publishRepo = artifactRepos.headOption.map(_.mavenResolver("publish"))
    val credentials = artifactRepos.map(_.credentials)
    
    debug(s"Configured ${pullRepos.size} pull repositories")
    debug(s"Configured publish repository: ${publishRepo.map(_.name).getOrElse("none")}")
    debug(s"Configured ${credentials.size} credential(s)")
    
    Seq(
      Keys.credentials ++= credentials,
      Keys.resolvers ++= pullRepos,
      Keys.publishTo := publishRepo
    )
  }


  lazy val artifactRepos: Seq[ArtifactRepoConfig] = {
    debug(s"Searching for .artifactrepo files in user home: ${Path.userHome}")
    
    val configFiles = Path.userHome.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = {
        name.endsWith(".artifactrepo")
      }
    })
    
    debug(s"Found ${configFiles.length} .artifactrepo file(s)")
    
    val results = configFiles.map(file => (file, readRepoConfig(file)))
    val (successfulConfigs, failedConfigs) = results.partition(_._2.isRight)
    
    debug(s"Successfully parsed ${successfulConfigs.length} configuration(s)")
    debug(s"Failed to parse ${failedConfigs.length} configuration(s)")
    
    // Log successful configuration loads
    successfulConfigs.foreach {
      case (file, Right(config)) =>
        println(s"[sbt-artifact-repo] Loaded configuration from: $file")
        debug(s"  Configuration details: host=${config.host}, pullRepo=${config.pullRepo}, publishRepo=${config.publishRepo}, protocol=${config.protocol}")
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
    debug(s"Attempting to read configuration from: $file")
    
    Try {
      val props = new Properties()
      props.load(new FileInputStream(file))
      val keys = props.stringPropertyNames().asScala.toSet
      
      debug(s"  Properties found: ${keys.mkString(", ")}")
      
      val requiredKeys = Set("realm", "host", "user", "password", "pull-repo", "publish-repo")
      val missingKeys = requiredKeys -- keys
      
      if (missingKeys.nonEmpty) {
        debug(s"  Missing required properties: ${missingKeys.mkString(", ")}")
        Left(s"Configuration file '$file' is missing required properties: ${missingKeys.mkString(", ")}")
      } else {
        debug(s"  All required properties present")
        debug(s"  Creating configuration for host: ${props.getProperty("host")}")
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
      case Failure(exception) => 
        debug(s"  Exception reading file: ${exception.getClass.getSimpleName}: ${exception.getMessage}")
        Left(s"Failed to read configuration file '$file': ${exception.getMessage}")
    }
  }
}

