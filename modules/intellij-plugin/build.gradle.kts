plugins {
    java
    id("org.jetbrains.intellij") version "1.17.4"
}

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf("org.intellij.intelliLang", "org.jetbrains.plugins.textmate"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("")
}
