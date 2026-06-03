# Publishing

Publishing is split into Maven artifacts and the IntelliJ plugin.

## Maven Local

Use Maven local while testing snapshots in another project:

```powershell
.\gradlew.bat publishToMavenLocal
```

In the consumer project:

```kotlin
repositories {
    mavenLocal() // remove once using a released version
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

Current development version:

```text
0.0.4-SNAPSHOT
```

Publish with the configured Gradle publishing tasks after setting credentials and signing.

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
