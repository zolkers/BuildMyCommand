// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

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
          <li>Route and SubRoute DSL highlighting.</li>
          <li>Requirement expression highlighting for @Require.</li>
          <li>Inspections for common BuildMyCommand annotation mistakes.</li>
          <li>Suggestion provider validation.</li>
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

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandRouteCompletionContributor*")
        }
    }))
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("dev/riege/buildmycommand/intellij/BuildMyCommandRouteCompletionContributor*")
        }
    }))
}
