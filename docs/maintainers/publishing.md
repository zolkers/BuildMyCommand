<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Publishing

Publishing is split into Maven artifacts and the IntelliJ plugin.

This page is for maintainers and contributors. Application users should consume released artifacts from Maven Central as shown in [Getting Started](../getting-started/installation.md).

## Local Development

Use Maven local only when you are developing BuildMyCommand itself and need to test unpublished changes in another project:

```powershell
.\gradlew.bat publishToMavenLocal
```

In the consumer project:

```kotlin
repositories {
    mavenLocal() // contributor workflow only
    mavenCentral()
}
```

## Maven Central Credentials

Store credentials in your user Gradle properties, not in the repository:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKey=...
signingInMemoryKeyPassword=...
```

On Windows, this is usually:

```text
C:\Users\<you>\.gradle\gradle.properties
```

The helper script can set Maven Central credentials:

```powershell
.\scripts\setup-maven-central-credentials.ps1
```

## Maven Central Publish

The project uses group:

```text
io.github.zolkers
```

Current release line:

```text
0.3.3
```

Snapshot versions and `mavenLocal()` are not part of the public installation path. Before publishing, set the Gradle project version to the release version on the development branch, open a pull request into `master`, then merge it.

## GitHub Release Workflow

CI publishes Maven artifacts and creates a GitHub Release automatically when `master` receives a push, usually through a merged pull request.

The version is read from the Gradle project version. The workflow creates the Git tag automatically:

| Gradle version | Created tag |
| --- | --- |
| `0.3.3` | `v0.3.3` |
| `0.3.2` | `v0.3.2` |
| `1.2.3` | `v1.2.3` |

The workflow runs:

```bash
./gradlew clean check -PreleaseVersion=<version>
./gradlew publishAndReleaseToMavenCentral -PreleaseVersion=<version>
```

If `v<version>` already exists, the workflow skips publishing. This makes ordinary documentation or maintenance merges into `master` safe when the version has not changed.

Configure these repository secrets before relying on the workflow:

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Maven Central username/token username. |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central password/token password. |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored GPG private key. |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | Password for the signing key. |

Do not push release tags manually. Tags are an output of the `master` release workflow, not the trigger.

## Branch Workflow

Use `ddev` for development work:

```bash
git switch ddev
```

For each new version:

1. Implement changes on `ddev`.
2. Update the Gradle version and public docs to the new version.
3. Open a pull request from `ddev` to `master`.
4. Merge the pull request into `master`.
5. Let CI create `v<version>`, publish Maven Central, and create the GitHub Release.

## IntelliJ Plugin Local Release

Build and install locally:

```powershell
.\gradlew.bat installIntellijPluginLocal
```

The plugin zip is produced under:

```text
modules/intellij-plugin/build/distributions/
```

Restart IntelliJ after installing.

## IntelliJ Marketplace Publish

Configure a JetBrains Marketplace token outside the repository:

```powershell
$env:JETBRAINS_MARKETPLACE_TOKEN="perm:..."
```

Persist it locally and as a GitHub Actions repository secret:

```powershell
.\scripts\setup-jetbrains-marketplace-token.ps1
```

Publish the plugin:

```powershell
.\gradlew.bat :intellij-plugin:publishPlugin
```

Optional channel:

```powershell
$env:JETBRAINS_MARKETPLACE_CHANNEL="eap"
```

The Gradle task also accepts `-PjetbrainsMarketplaceToken=...` for local one-off publishing. Use the `JETBRAINS_MARKETPLACE_TOKEN` repository secret for CI automation.

## Release Checklist

| Step | Check |
| --- | --- |
| Tests | `.\gradlew.bat clean check` passes. |
| Maven local | Consumer project can resolve artifacts. |
| Fabric smoke test | Client command registers, executes, suggests. |
| Docs | README and docs match current version/API. |
| IntelliJ plugin | Installs locally, highlights routes, no startup crash. |
| Version | Snapshot changed to release version before Central publish. |
