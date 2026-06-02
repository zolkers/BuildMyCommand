plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:core"))
    api(project(":api"))
    api(project(":core"))
    api("com.mojang:brigadier:1.0.18")
}
