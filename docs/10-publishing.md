# 10 - Publishing

This page documents release and publication concerns for framework artifacts and the IntelliJ plugin.

## Current Coordinates

| Field | Value |
| --- | --- |
| Group | `io.github.zolkers` |
| Version | `0.0.1` |
| Java toolchain | 21 for framework modules, 17 bytecode for IntelliJ plugin |
| Maven Central plugin | `com.vanniktech.maven.publish` `0.36.0` |
| Local Maven repository | `build/repo` per published module |
| Repository URL | `https://github.com/zolkers/BuildMyCommand` |

The root Gradle build applies publishing only to releaseable framework modules. Aggregators, examples, and the IntelliJ plugin are intentionally excluded from Maven Central artifacts.

## Artifact Names

Artifact ids are derived from Gradle paths by replacing `:` with `-`.

| Gradle project | Maven coordinate |
| --- | --- |
| `:api` | `io.github.zolkers:api:0.0.1` |
| `:core` | `io.github.zolkers:core:0.0.1` |
| `:annotations` | `io.github.zolkers:annotations:0.0.1` |
| `:dsl` | `io.github.zolkers:dsl:0.0.1` |
| `:schema` | `io.github.zolkers:schema:0.0.1` |
| `:testkit` | `io.github.zolkers:testkit:0.0.1` |
| `:adapters:core` | `io.github.zolkers:adapters-core:0.0.1` |
| `:adapters:brigadier` | `io.github.zolkers:adapters-brigadier:0.0.1` |
| `:adapters:minecraft:common` | `io.github.zolkers:adapters-minecraft-common:0.0.1` |
| `:adapters:minecraft:fabric` | `io.github.zolkers:adapters-minecraft-fabric:0.0.1` |

## User Install

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.zolkers:api:0.0.1")
    implementation("io.github.zolkers:core:0.0.1")
    implementation("io.github.zolkers:annotations:0.0.1")
    implementation("io.github.zolkers:adapters-brigadier:0.0.1")
}
```

Minecraft users should import only the adapter matching their platform:

| Platform | Dependency |
| --- | --- |
| Generic Brigadier runtime | `io.github.zolkers:adapters-brigadier:0.0.1` |
| Fabric | `io.github.zolkers:adapters-minecraft-fabric:0.0.1` |

## Local Publish

Publish one module locally:

```powershell
.\gradlew.bat :api:publishAllPublicationsToBuildDirectoryRepository
```

Publish a set of modules locally:

```powershell
.\gradlew.bat :api:publishAllPublicationsToBuildDirectoryRepository :core:publishAllPublicationsToBuildDirectoryRepository :annotations:publishAllPublicationsToBuildDirectoryRepository
```

Artifacts are written under each module's `build/repo` directory. Local publishing does not require Maven Central credentials or signing keys.

## Maven Central Release

Before the first release, verify the `io.github.zolkers` namespace in Sonatype Central Portal. Releases must be immutable; never publish `0.0.1` until the API and docs are intentionally frozen.

| Secret or property | Purpose |
| --- | --- |
| `mavenCentralUsername` | Sonatype Central Portal user token name. |
| `mavenCentralPassword` | Sonatype Central Portal user token password. |
| `signingInMemoryKey` | ASCII-armored GPG private key used to sign publications. |
| `signingInMemoryKeyPassword` | Password for the in-memory signing key. |

## Local Credential Setup

Never commit Maven Central credentials. Store them in the Gradle user home file:

| OS | File |
| --- | --- |
| Windows | `C:\Users\<you>\.gradle\gradle.properties` |
| macOS/Linux | `~/.gradle/gradle.properties` |
| Custom Gradle home | `$GRADLE_USER_HOME/gradle.properties` |

Use the setup scripts to write or update the safe local file.

With explicit username/password on Windows:

```powershell
.\scripts\setup-maven-central-credentials.ps1 `
  -Username "central-token-username" `
  -Password "central-token-password"
```

With the base64 value of `username:password` on Windows:

```powershell
.\scripts\setup-maven-central-credentials.ps1 `
  -TokenBase64 "BASE64_USERNAME_COLON_PASSWORD"
```

With explicit username/password on macOS/Linux:

```bash
./scripts/setup-maven-central-credentials.sh \
  --username "central-token-username" \
  --password "central-token-password"
```

With the base64 value of `username:password` on macOS/Linux:

```bash
./scripts/setup-maven-central-credentials.sh \
  --token-base64 "BASE64_USERNAME_COLON_PASSWORD"
```

If you also want local signing credentials, pass an ASCII-armored private key file and its password:

```powershell
.\scripts\setup-maven-central-credentials.ps1 `
  -TokenBase64 "BASE64_USERNAME_COLON_PASSWORD" `
  -SigningInMemoryKeyFile "$HOME\private-signing-key.asc" `
  -SigningInMemoryKeyPassword "key-password"
```

```bash
./scripts/setup-maven-central-credentials.sh \
  --token-base64 "BASE64_USERNAME_COLON_PASSWORD" \
  --signing-in-memory-key-file "$HOME/private-signing-key.asc" \
  --signing-in-memory-key-password "key-password"
```

The scripts do not print secret values. They only print the file path and property names that were updated.

Recommended CI environment variables:

```text
ORG_GRADLE_PROJECT_mavenCentralUsername=...
ORG_GRADLE_PROJECT_mavenCentralPassword=...
ORG_GRADLE_PROJECT_signingInMemoryKey=...
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

Release commands:

```powershell
.\gradlew.bat check
.\gradlew.bat publishToMavenCentral
.\gradlew.bat publishAndReleaseToMavenCentral
```

`publishToMavenCentral` uploads a deployment. `publishAndReleaseToMavenCentral` publishes and releases it when credentials, namespace, POM metadata, javadocs, sources, and signatures are valid.

## IntelliJ Plugin Release

| Task | Purpose |
| --- | --- |
| `:intellij-plugin:buildPlugin` | Builds ZIP for manual install/deploy. |
| `installIntellijPluginLocal` | Installs plugin into local IntelliJ config. |
| `:intellij-plugin:publishPlugin` | Publishes to JetBrains Marketplace when token is configured. |

The plugin currently targets IntelliJ platform `2024.1` with `sinceBuild = 241`.

## Release Checklist

| Check | Command |
| --- | --- |
| Full verification | `.\gradlew.bat check` |
| Build plugin ZIP | `.\gradlew.bat :intellij-plugin:buildPlugin` |
| Local Maven smoke test | `.\gradlew.bat :api:publishAllPublicationsToBuildDirectoryRepository` |
| Inspect git status | `git status --short --branch` |
| Tag release | `git tag v0.0.1` |

## Versioning Policy

| Version type | Meaning |
| --- | --- |
| `0.0.x` | Early experimental releases; API can still change. |
| `0.x.0` | Feature milestones; migration notes required. |
| `1.0.0` | Stable public contracts for API/core/adapters. |

Document breaking changes in the release notes before publishing.
