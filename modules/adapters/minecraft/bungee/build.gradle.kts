// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:minecraft:common"))
    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")
    testImplementation("net.md-5:bungeecord-api:1.21-R0.2")
}
