# 10 - Publishing

This page documents release and publication concerns for framework artifacts and the IntelliJ plugin.

## Current Coordinates

| Field | Value |
| --- | --- |
| Group | `dev.riege.buildmycommand` |
| Version | `0.0.1` |
| Java toolchain | 21 for framework modules, 17 bytecode for IntelliJ plugin |
| Publication task | Gradle `maven-publish` |
| Local Maven repository | `build/repo` |

Artifact ids are derived from Gradle paths:

| Gradle project | Artifact id |
| --- | --- |
| `:api` | `api` |
| `:core` | `core` |
| `:annotations` | `annotations` |
| `:adapters:core` | `adapters-core` |
| `:adapters:brigadier` | `adapters-brigadier` |
| `:adapters:minecraft:paper` | `adapters-minecraft-paper` |

## Local Publish

```powershell
.\gradlew.bat publish
```

The configured repository named `buildDirectory` writes artifacts under `build/repo`.

## Maven Central Checklist

| Step | Requirement |
| --- | --- |
| Namespace | Own/verify the target namespace, for example `io.github.zolkers` if that is the chosen group. |
| Group id | Decide whether to keep `dev.riege.buildmycommand` or migrate to `io.github.zolkers`. |
| POM metadata | Name, description, URL, license, developer, SCM must match the real repository. |
| Signing | Configure GPG signing for Maven Central. |
| Repository | Add Central/Sonatype publishing repository. |
| CI secrets | Store publishing token and signing key securely. |
| Versioning | Tag release, publish immutable version. |

If Maven Central should be under `io.github.zolkers`, update `group` in root `build.gradle.kts` and POM SCM metadata before release.

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
| Local Maven publish | `.\gradlew.bat publish` |
| Inspect git status | `git status --short --branch` |
| Tag release | `git tag v0.0.1` |

## Versioning Policy

| Version type | Meaning |
| --- | --- |
| `0.0.x` | Early experimental releases; API can still change. |
| `0.x.0` | Feature milestones; migration notes required. |
| `1.0.0` | Stable public contracts for API/core/adapters. |

Document breaking changes in the release notes before publishing.
