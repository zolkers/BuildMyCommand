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
0.0.4
```

Snapshot versions and `mavenLocal()` are not part of the public installation path. Before publishing, set the Gradle project version to the release version, run the full checks, publish, then move development back to the next `-SNAPSHOT`.

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
