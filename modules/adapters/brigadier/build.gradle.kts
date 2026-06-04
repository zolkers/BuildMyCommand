plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:core"))
    api("com.mojang:brigadier:1.0.18")
    testImplementation(project(":annotations"))
}
