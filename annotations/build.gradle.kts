plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    testImplementation(project(":core"))
}
