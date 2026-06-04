// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:core"))
    api("com.mojang:brigadier:1.0.18")
    testImplementation(project(":annotations"))
}
