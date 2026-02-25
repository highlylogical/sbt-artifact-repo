libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Compile

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.highlylogical.oss" % "sbt-artifact-repo" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

