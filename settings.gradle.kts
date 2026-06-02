pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
}

rootProject.name = "buildmycommand"

include("api")
include("annotations")
include("adapters")
include("adapters:minecraft")
include("adapters:minecraft:bungee")
include("adapters:minecraft:common")
include("adapters:minecraft:fabric")
include("adapters:minecraft:forge")
include("adapters:minecraft:minestom")
include("adapters:minecraft:neoforge")
include("adapters:minecraft:paper")
include("adapters:minecraft:sponge")
include("adapters:minecraft:spigot")
include("adapters:minecraft:velocity")
include("core")
include("discord-adapter")
include("dsl")
include("examples")
include("intellij-plugin")
include("schema")
include("terminal-adapter")
include("testkit")

project(":api").projectDir = file("modules/api")
project(":annotations").projectDir = file("modules/annotations")
project(":adapters").projectDir = file("modules/adapters")
project(":adapters:minecraft").projectDir = file("modules/adapters/minecraft")
project(":adapters:minecraft:bungee").projectDir = file("modules/adapters/minecraft/bungee")
project(":adapters:minecraft:common").projectDir = file("modules/adapters/minecraft/common")
project(":adapters:minecraft:fabric").projectDir = file("modules/adapters/minecraft/fabric")
project(":adapters:minecraft:forge").projectDir = file("modules/adapters/minecraft/forge")
project(":adapters:minecraft:minestom").projectDir = file("modules/adapters/minecraft/minestom")
project(":adapters:minecraft:neoforge").projectDir = file("modules/adapters/minecraft/neoforge")
project(":adapters:minecraft:paper").projectDir = file("modules/adapters/minecraft/paper")
project(":adapters:minecraft:sponge").projectDir = file("modules/adapters/minecraft/sponge")
project(":adapters:minecraft:spigot").projectDir = file("modules/adapters/minecraft/spigot")
project(":adapters:minecraft:velocity").projectDir = file("modules/adapters/minecraft/velocity")
project(":core").projectDir = file("modules/core")
project(":discord-adapter").projectDir = file("modules/discord-adapter")
project(":dsl").projectDir = file("modules/dsl")
project(":examples").projectDir = file("modules/examples")
project(":intellij-plugin").projectDir = file("modules/intellij-plugin")
project(":schema").projectDir = file("modules/schema")
project(":terminal-adapter").projectDir = file("modules/terminal-adapter")
project(":testkit").projectDir = file("modules/testkit")
