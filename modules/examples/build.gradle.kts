plugins {
    java
}

dependencies {
    implementation(project(":annotations"))
    implementation(project(":adapters:core"))
    implementation(project(":adapters:terminal"))
    implementation(project(":core"))
}
