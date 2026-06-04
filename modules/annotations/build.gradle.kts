// Copyright (c) 2026 Zolkers
//
// Licensed under the MIT License.
// SPDX-License-Identifier: MIT

plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    implementation(project(":core"))
    implementation(project(":dsl"))
    testImplementation(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
