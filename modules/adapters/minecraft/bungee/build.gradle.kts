plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:minecraft:common"))
    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")
    testImplementation("net.md-5:bungeecord-api:1.21-R0.2")
}
