# Publishing

Publishing is split into Maven artifacts and the IntelliJ plugin.

This page is for maintainers and contributors. Application users should consume released artifacts from Maven Central as shown in [Getting Started](01-getting-started.md).

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
0.1.1
```

Snapshot versions and `mavenLocal()` are not part of the public installation path. Before publishing, set the Gradle project version to the release version, run the full checks, publish, then move development back to the next `-SNAPSHOT`.

## GitHub Release Workflow

CI publishes Maven artifacts and creates a GitHub Release automatically when a tag matching `v*` is pushed. The version is derived from the tag name:

| Tag | Published version |
| --- | --- |
| `v0.1.1` | `0.1.1` |
| `v1.2.3` | `1.2.3` |

The workflow runs:

```bash
./gradlew clean check -PreleaseVersion=<version>
./gradlew publishAndReleaseToMavenCentral -PreleaseVersion=<version>
```

Configure these repository secrets before relying on the workflow:

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Maven Central username/token username. |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central password/token password. |
| `SIGNING_IN_MEMORY_KEY` | ASCII-armored GPG private key. |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | Password for the signing key. |

GitHub Actions only reacts to tag pushes that happen after the workflow exists. If a tag already exists, create the GitHub Release manually or move the tag only when the Maven version has not already been published.

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

## Release Checklist

| Step | Check |
| --- | --- |
| Tests | `.\gradlew.bat clean check` passes. |
| Maven local | Consumer project can resolve artifacts. |
| Fabric smoke test | Client command registers, executes, suggests. |
| Docs | README and docs match current version/API. |
| IntelliJ plugin | Installs locally, highlights routes, no startup crash. |
| Version | Snapshot changed to release version before Central publish. |
