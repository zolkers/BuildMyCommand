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
include("adapters:brigadier")
include("adapters:core")
include("adapters:minecraft")
include("adapters:minecraft:common")
include("adapters:minecraft:fabric")
include("core")
include("dsl")
include("examples")
include("intellij-plugin")
include("schema")
include("testkit")

project(":api").projectDir = file("modules/api")
project(":annotations").projectDir = file("modules/annotations")
project(":adapters").projectDir = file("modules/adapters")
project(":adapters:brigadier").projectDir = file("modules/adapters/brigadier")
project(":adapters:core").projectDir = file("modules/adapters/core")
project(":adapters:minecraft").projectDir = file("modules/adapters/minecraft")
project(":adapters:minecraft:common").projectDir = file("modules/adapters/minecraft/common")
project(":adapters:minecraft:fabric").projectDir = file("modules/adapters/minecraft/fabric")
project(":core").projectDir = file("modules/core")
project(":dsl").projectDir = file("modules/dsl")
project(":examples").projectDir = file("modules/examples")
project(":intellij-plugin").projectDir = file("modules/intellij-plugin")
project(":schema").projectDir = file("modules/schema")
project(":testkit").projectDir = file("modules/testkit")
