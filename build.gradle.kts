import java.math.BigDecimal

plugins {
    java
    jacoco
}

group = "dev.riege.buildmycommand"
version = "0.0.1"

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

subprojects {
    if (path in adapterAggregatorProjects) {
        return@subprojects
    }

    apply(plugin = "java")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
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
        dependsOn(tasks.named("test"))
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

    apply(from = rootProject.file("gradle/publishing.gradle.kts"))
}
