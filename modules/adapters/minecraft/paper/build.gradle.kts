plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:minecraft:common"))
    api(project(":adapters:minecraft:spigot"))
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
