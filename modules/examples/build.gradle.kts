plugins {
    java
}

dependencies {
    implementation(project(":annotations"))
    implementation(project(":adapters:brigadier"))
    implementation(project(":adapters:core"))
    implementation(project(":adapters:minecraft:common"))
    implementation(project(":adapters:terminal"))
    implementation(project(":core"))
    implementation(project(":testkit"))
}
