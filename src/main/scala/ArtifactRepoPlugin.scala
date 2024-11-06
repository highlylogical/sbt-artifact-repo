package com.highlylogical.oss

import sbt.io.Pat
import sbt.toRepositoryName
import sbt.{AutoPlugin, Credentials, Def, Keys, MavenRepository}

import java.io.{File, FileInputStream, FilenameFilter}
import java.util.Properties

case class ArtifactRepoConfig(host: String, publishRepo: String, pullRepo: String, credentials: Credentials) {
  def mavenResolver(direction: String): MavenRepository = s"$direction-repo" at s"https://$host/$pullRepo"
}

object ArtifactRepoPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = sbt.plugins.JvmPlugin

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    Keys.credentials ++= artifactRepos.map(_.credentials),
    Keys.resolvers ++= artifactRepos.map(_.mavenResolver("pull")),
    Keys.publishTo := artifactRepos.headOption.map(_.mavenResolver("publish"))
  )


  lazy val artifactRepos: Seq[ArtifactRepoConfig] = {
    val configFiles = Path.userHome.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = {
        println(s"check file $name")
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
    val keys = props.keySet().asScala.toSet
    print(s"Loaded props from $file: $keys")
    if (keys == Set("realm", "host", "user", "password", "pull-repo", "publish-repo")) {
      Right(ArtifactRepoConfig(
        props.getProperty("host"),
        props.getProperty("publish-repo"),
        props.getProperty("pull-repo"),
        Credentials(
          props.getProperty("realm"),
          props.getProperty("host"),
          props.getProperty("user"),
          props.getProperty("password")
        )
      ))
    } else {

      Left(s"Not all properties present")
    }
    }
}

