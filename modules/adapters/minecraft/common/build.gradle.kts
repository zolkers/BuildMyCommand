plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:brigadier"))
    api(project(":adapters:core"))
    api(project(":api"))
    api(project(":core"))
}
