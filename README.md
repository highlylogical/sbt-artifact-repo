# sbt-artifact-repo
This plugin supports configuring artifact repos using a configuration file. Create a file such as

```
realm=Artifactory Realm
host=repo.acme.com
user=acme_user
password=user_pat
pull-repo=maven-virtual
publish-repo=maven-local
```

Config path defaults to the user home directory and the file pattern to `.*\.artifactrepo`. Override in your build with `artifactRepoConfigPath` and `artifactRepoConfigFilePattern`.

Add the plugin to `project/plugins.sbt` for regular project dependencies, and to `project/project/plugins.sbt` for plugins used in the build.