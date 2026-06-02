plugins {
    java
    jacoco
}

group = "dev.riege.buildmycommand"
version = "0.1.0-SNAPSHOT"

tasks.register<Exec>("setupIntellijPlugin") {
    group = "ide"
    description = "Declares the BuildMyCommand IntelliJ plugin as required for this project and builds the local plugin ZIP."
    dependsOn(":intellij-plugin:buildPlugin")

    val script = layout.projectDirectory.file("scripts/setup-intellij-plugin.ps1")
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.asFile.absolutePath, "-SkipBuild")
}

subprojects {
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

    if (name != "examples" && name != "intellij-plugin") {
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("test"))
            violationRules {
                rule {
                    limit {
                        minimum = "0.20".toBigDecimal()
                    }
                }
            }
        }

        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
        }
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:5.13.1"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
