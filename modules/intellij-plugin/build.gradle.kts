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
    sinceBuild.set("241")
    untilBuild.set("")
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
