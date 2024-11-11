package com.highlylogical.oss

import sbt.io.Path
import sbt.toRepositoryName
import sbt.{AutoPlugin, Credentials, Def, Keys, MavenRepository}

import java.io.{File, FileInputStream, FilenameFilter}
import java.util.Properties

case class ArtifactRepoConfig(host: String, publishRepo: String, pullRepo: String, credentials: Credentials, protocol: String = "https") {
  def mavenResolver(direction: String): MavenRepository = s"$direction-repo" at s"$protocol://$host/$pullRepo"
}

object ArtifactRepoPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = sbt.plugins.JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = {
    val pullRepos = artifactRepos.map(_.mavenResolver("pull"))
    val publishRepo = artifactRepos.headOption.map(_.mavenResolver("publish"))
    val credentials = artifactRepos.map(_.credentials)
    println(s"Adding artifact repos to resolvers: $pullRepos")
    println(s"Adding artifact repo as publish repo: $publishRepo")
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
    configFiles.map(readRepoConfig).collect {
      case Right(config) => config
    }
  }

  import scala.collection.JavaConverters._
  def readRepoConfig(file: File): Either[String, ArtifactRepoConfig]  = {
    val props = new Properties()
    props.load(new FileInputStream(file))
    val keys = props.stringPropertyNames().asScala.toSet
    print(s"Loaded props from $file: $keys")
    if (Set("realm", "host", "user", "password", "pull-repo", "publish-repo").subsetOf(keys)) {
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
    } else {
      Left(s"Not all properties present")
    }
    }
}

