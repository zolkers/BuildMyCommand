// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:minecraft:common"))
    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
    testImplementation("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}
