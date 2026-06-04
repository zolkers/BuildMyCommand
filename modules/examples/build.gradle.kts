// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    java
}

dependencies {
    implementation(project(":annotations"))
    implementation(project(":adapters:brigadier"))
    implementation(project(":adapters:core"))
    implementation(project(":adapters:minecraft:common"))
    implementation(project(":adapters:minecraft:fabric"))
    implementation(project(":adapters:minecraft:forge"))
    implementation(project(":adapters:minecraft:neoforge"))
    implementation(project(":adapters:terminal"))
    implementation(project(":core"))
    implementation(project(":testkit"))
}
