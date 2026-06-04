// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:minecraft:common"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    testImplementation("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
