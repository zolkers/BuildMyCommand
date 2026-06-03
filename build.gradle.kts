import java.math.BigDecimal
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    java
    jacoco
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

group = "io.github.zolkers"
version = "0.0.3-SNAPSHOT"

val mainJavaSources = fileTree(layout.projectDirectory.dir("modules")) {
    include("**/src/main/java/**/*.java")
}

tasks.register("qualityStyle") {
    group = "verification"
    description = "Checks repository Java source style without external tooling."
    inputs.files(mainJavaSources)

    doLast {
        val violations = mutableListOf<String>()
        mainJavaSources.files.sortedBy { it.path }.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.endsWith(" ") || line.endsWith("\t")) {
                    violations.add("${file.relativeTo(rootDir)}:${index + 1}: trailing whitespace")
                }
                if ('\t' in line) {
                    violations.add("${file.relativeTo(rootDir)}:${index + 1}: tab indentation")
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Style violations:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.register("qualityStaticAnalysis") {
    group = "verification"
    description = "Runs lightweight static checks for TODOs and oversized Java classes."
    inputs.files(mainJavaSources)

    doLast {
        val violations = mutableListOf<String>()
        val maxClassLines = 700
        mainJavaSources.files.sortedBy { it.path }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if ("TODO" in line || "FIXME" in line) {
                    violations.add("${file.relativeTo(rootDir)}:${index + 1}: unresolved TODO/FIXME")
                }
            }
            if (lines.size > maxClassLines) {
                violations.add("${file.relativeTo(rootDir)}:1: file has ${lines.size} lines; max is $maxClassLines")
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Static analysis violations:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.named("check") {
    dependsOn("qualityStyle", "qualityStaticAnalysis")
}

tasks.register<Exec>("setupIntellijPlugin") {
    group = "ide"
    description = "Declares the BuildMyCommand IntelliJ plugin as required for this project and builds the local plugin ZIP."
    dependsOn(":intellij-plugin:buildPlugin")

    val script = layout.projectDirectory.file("scripts/setup-intellij-plugin.ps1")
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.asFile.absolutePath, "-SkipBuild")
}

tasks.register<Exec>("installIntellijPluginLocal") {
    group = "ide"
    description = "Builds and installs the BuildMyCommand IntelliJ plugin into the latest local IntelliJ IDEA config directory."
    dependsOn(":intellij-plugin:buildPlugin")

    val script = layout.projectDirectory.file("scripts/setup-intellij-plugin.ps1")
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.asFile.absolutePath, "-SkipBuild", "-Install")
}

val adapterAggregatorProjects = setOf(":adapters", ":adapters:minecraft")
val nonPublishedProjects = setOf(":examples", ":intellij-plugin")
val publishableProjectExclusions = adapterAggregatorProjects + nonPublishedProjects

subprojects {
    if (path in adapterAggregatorProjects) {
        return@subprojects
    }

    group = if (path == ":adapters:core") "${rootProject.group}.adapters" else rootProject.group
    version = rootProject.version
    val artifact = path.removePrefix(":").replace(':', '-')

    apply(plugin = "java")
    apply(plugin = "jacoco")

    configure<BasePluginExtension> {
        archivesName.set(artifact)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        if (path in publishableProjectExclusions) {
            withSourcesJar()
            withJavadocJar()
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal.ONE
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:5.13.1"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    if (path !in publishableProjectExclusions) {
        val repositoryUrl = "https://github.com/zolkers/BuildMyCommand"
        val signingConfigured = providers.gradleProperty("signingInMemoryKey").isPresent
            || providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent
            || providers.gradleProperty("signing.secretKeyRingFile").isPresent
            || providers.environmentVariable("ORG_GRADLE_PROJECT_signing.secretKeyRingFile").isPresent

        apply(plugin = "com.vanniktech.maven.publish")

        configure<MavenPublishBaseExtension> {
            coordinates(rootProject.group.toString(), artifact, rootProject.version.toString())
            publishToMavenCentral()
            if (signingConfigured) {
                signAllPublications()
            }

            pom {
                name.set("BuildMyCommand $artifact")
                description.set("A modular Java command framework module.")
                inceptionYear.set("2026")
                url.set(repositoryUrl)
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("zolkers")
                        name.set("Zolkers")
                        url.set("https://github.com/zolkers")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/zolkers/BuildMyCommand.git")
                    developerConnection.set("scm:git:ssh://git@github.com/zolkers/BuildMyCommand.git")
                    url.set(repositoryUrl)
                }
            }
        }

        configure<PublishingExtension> {
            publications.withType(MavenPublication::class.java).configureEach {
                groupId = rootProject.group.toString()
                artifactId = artifact
                version = rootProject.version.toString()
            }

            repositories {
                maven {
                    name = "buildDirectory"
                    url = layout.buildDirectory.dir("repo").get().asFile.toURI()
                }
            }
        }
    }
}
