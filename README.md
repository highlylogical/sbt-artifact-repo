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
Add the plugin to `project/plugins.sbt` for regular project dependencies, and to `project/project/plugins.sbt` for plugins used in the build.

## Debug Logging

To enable detailed debug logging showing what the plugin is doing, start SBT with the debug flag:

```bash
sbt -Dsbt.artifact.repo.debug=true
```

This will show:
- Which configuration files are being searched and loaded
- What properties are found in each file
- Which properties are missing (if any)
- Configuration details for successfully loaded repos
- Summary of configured repositories and credentials

Example debug output:
```
[sbt-artifact-repo][DEBUG] Initializing artifact repository plugin
[sbt-artifact-repo][DEBUG] Searching for .artifactrepo files in user home: /home/user
[sbt-artifact-repo][DEBUG] Found 1 .artifactrepo file(s)
[sbt-artifact-repo][DEBUG] Attempting to read configuration from: /home/user/.artifactrepo/myrepo.artifactrepo
[sbt-artifact-repo][DEBUG]   Properties found: pull-repo, host, publish-repo, realm, user, protocol, password
[sbt-artifact-repo][DEBUG]   All required properties present
[sbt-artifact-repo][DEBUG]   Creating configuration for host: repo.acme.com
[sbt-artifact-repo] Loaded configuration from: /home/user/.artifactrepo/myrepo.artifactrepo
[sbt-artifact-repo][DEBUG]   Configuration details: host=repo.acme.com, pullRepo=maven-virtual, publishRepo=maven-local, protocol=https
```

## Development

### Running Tests

The plugin has both unit tests and integration tests using the SBT scripted framework.

**Unit tests:**
```bash
sbt test
```

**Scripted integration tests:**
```bash
sbt scripted
```

Run a specific scripted test:
```bash
sbt "scripted sbt-artifact-repo/valid-config"
```

The scripted tests include:
- `valid-config`: Tests loading a valid configuration file
- `missing-properties`: Tests handling incomplete configuration files
- `no-config`: Tests that the plugin works when no configuration file exists