package com.highlylogical.oss

import sbt.{AutoPlugin, Credentials, Def, Keys, MavenRepository}
import sbt._
import sbt.util.Logger

import java.io.{File, FileInputStream}
import java.util.Properties
import scala.util.{Try, Success, Failure}

case class ArtifactRepoConfig(host: String, publishRepo: String, pullRepo: String, credentials: Credentials, protocol: String = "https") {
  def mavenResolver(direction: String): MavenRepository = {
    val repo = if (direction == "pull") pullRepo else publishRepo
    val mavenRepo = s"$repo-$direction" at s"$protocol://$host/$repo"
    if (protocol == "http") {
      mavenRepo.withAllowInsecureProtocol(true)
    } else {
      mavenRepo    
    }
  }
}

object ArtifactRepoPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = sbt.plugins.JvmPlugin

  object autoImport {
    val artifactRepoConfigPath = settingKey[File]("Path to location of artifact repo configurations")
    val artifactRepoConfigFilePattern = settingKey[String]("Pattern match for config files")
  }

  import autoImport._

  private val artifactRepoConfigs = settingKey[Seq[ArtifactRepoConfig]]("Loaded artifact repo configs (cached per project)")

  lazy val artifactRepoSettings: Seq[Def.Setting[_]] = Seq(
    artifactRepoConfigFilePattern := ".*\\.artifactrepo",
    artifactRepoConfigPath := sbt.io.Path.userHome
  )

  override def globalSettings: Seq[Setting[_]] = artifactRepoSettings

  def loadConfig(path: File, pattern: String, log: Logger): Seq[ArtifactRepoConfig] = {
    val files = Option(path.listFiles()).getOrElse(Array.empty[File])
    files.filter(f => f.isFile && f.getName.matches(pattern)).flatMap { f =>
      readRepoConfig(f) match {
        case Right(config) => Some(config)
        case Left(err) =>
          log.warn(s"Invalid artifact repo config $f: $err")
          None
      }
    }.toSeq
  }

  def loadCredentials(path: File, pattern: String, log: Logger): Seq[Credentials] = {
    loadConfig(path, pattern, log).map(_.credentials)
  }

  def loadPublishRepo(path: File, pattern: String, log: Logger): Option[MavenRepository] = {
    loadConfig(path, pattern, log).headOption.map(_.mavenResolver("publish"))
  }

  def loadResolvers(path: File, pattern: String, log: Logger): Seq[MavenRepository] = {
    loadConfig(path, pattern, log).map(_.mavenResolver("pull")).toSeq
  }

  override def projectSettings: Seq[Def.Setting[_]] = {
    Seq(
      artifactRepoConfigs := loadConfig(artifactRepoConfigPath.value, artifactRepoConfigFilePattern.value, Keys.sLog.value),
      Keys.credentials ++= artifactRepoConfigs.value.map(_.credentials),
      Keys.resolvers ++= artifactRepoConfigs.value.map(_.mavenResolver("pull")),
      Keys.publishTo := artifactRepoConfigs.value.headOption.map(_.mavenResolver("publish"))
    )
  }

  def readRepoConfig(file: File): Either[String, ArtifactRepoConfig] = {
    val props = new Properties()
    Try {
      var stream: FileInputStream = null
      try {
        stream = new FileInputStream(file)
        props.load(stream)
      } finally {
        if (stream != null) try { stream.close() } catch { case _: Exception => }
      }
      val keys = props.stringPropertyNames()
      val requiredKeys = Set("realm", "host", "user", "password", "pull-repo", "publish-repo")
      if (!requiredKeys.forall(keys.contains)) {
        Left("Not all properties present")
      } else {
        def get(key: String, default: String = ""): String = Option(props.getProperty(key)).getOrElse(default).trim
        val host = get("host")
        val publishRepo = get("publish-repo")
        val pullRepo = get("pull-repo")
        val realm = get("realm")
        val user = get("user")
        val password = get("password")
        if (host.isEmpty || publishRepo.isEmpty || pullRepo.isEmpty || realm.isEmpty || user.isEmpty || password.isEmpty) {
          Left("Required property missing or empty")
        } else {
          Right(ArtifactRepoConfig(
            host,
            publishRepo,
            pullRepo,
            Credentials(realm, host, user, password),
            props.getProperty("protocol", "https").trim match { case "" => "https"; case p => p }
          ))
        }
      }
    } match {
      case Success(either) => either
      case Failure(e) => Left(Option(e.getMessage).getOrElse(e.toString))
    }
  }
}

