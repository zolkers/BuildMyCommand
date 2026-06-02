plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    implementation(project(":dsl"))
    testImplementation(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
