// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":adapters:brigadier"))
    api(project(":adapters:core"))
    api(project(":api"))
    api(project(":core"))
}
