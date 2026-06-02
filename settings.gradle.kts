pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "buildmycommand"

include("api")
include("annotations")
include("core")
include("examples")
include("intellij-plugin")
include("minecraft-adapter")
include("terminal-adapter")
include("testkit")

project(":api").projectDir = file("modules/api")
project(":annotations").projectDir = file("modules/annotations")
project(":core").projectDir = file("modules/core")
project(":examples").projectDir = file("modules/examples")
project(":intellij-plugin").projectDir = file("modules/intellij-plugin")
project(":minecraft-adapter").projectDir = file("modules/minecraft-adapter")
project(":terminal-adapter").projectDir = file("modules/terminal-adapter")
project(":testkit").projectDir = file("modules/testkit")
