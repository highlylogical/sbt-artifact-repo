# sbt-artifact-repo
This plugin supports configuring artifact repos using a configuration file. Create a file such as

```
realm=Artifactory Realm
host=repo.acme.com
user=acme_user
password=user_pat
pullRepo=maven-virtual
pushRepo=maven-local
```
Add the plugin to `project/plugins.sbt` for regular project dependencies, and to `project/project/plugins.sbt` for plugins used in the build.