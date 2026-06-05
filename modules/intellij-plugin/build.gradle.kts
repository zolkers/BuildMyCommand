// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

val communityVerifierIdeVersions = providers.gradleProperty("pluginVerifierIdeVersions")
    .map { versions ->
        versions.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
    }
    .orElse(listOf("IC-2024.1", "IC-2024.3", "IC-2025.1", "IC-2025.2"))

plugins {
    java
    jacoco
    id("org.jetbrains.intellij") version "1.17.4"
}

repositories {
    mavenCentral()
}

dependencies {
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf("java", "org.intellij.intelliLang", "org.jetbrains.plugins.textmate"))
    instrumentCode.set(false)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.patchPluginXml {
    version.set(rootProject.version.toString())
    sinceBuild.set("241")
    untilBuild.set("")
    changeNotes.set(
        """
        <ul>
          <li>Removes the Route DSL completion space workaround so lookup items no longer appear duplicated with leading spaces.</li>
          <li>Keeps highlighting, inspections and completion active for @Route, @SubRoute, builder route/subRoute/path calls, @Require and regex permissions.</li>
          <li>Updates the required plugin metadata for BuildMyCommand 0.3.3 projects.</li>
        </ul>
        """.trimIndent()
    )
}

tasks.publishPlugin {
    token.set(
        providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
            .orElse(providers.gradleProperty("jetbrainsMarketplaceToken"))
    )
    channels.set(
        providers.environmentVariable("JETBRAINS_MARKETPLACE_CHANNEL")
            .map { listOf(it) }
            .orElse(listOf("default"))
    )
}

tasks.runPluginVerifier {
    ideVersions.set(communityVerifierIdeVersions)
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandFlatParser*")
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandInjectedFile*")
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandRouteCompletionContributor*")
            exclude("dev/riege/buildmycommand/intellij/*ParserDefinition*")
        }
    }))
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandFlatParser*")
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandInjectedFile*")
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandRouteCompletionContributor*")
            exclude("dev/riege/buildmycommand/intellij/*ParserDefinition*")
        }
    }))
}
