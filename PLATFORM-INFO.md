<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Platform Info

This file tracks the real validation status of BuildMyCommand platform adapters.

BuildMyCommand may contain source modules for several platforms before each platform has been validated in a real project. Treat the status below as the source of truth for what is considered ready to recommend.

## Status Legend

| Status | Meaning |
| --- | --- |
| Tested | Verified in a real project or with an end-to-end smoke test for the platform. |
| Contract-tested | Covered by shared adapter behavior tests, but not yet verified in a real platform project. |
| Compile-tested | The module compiles and has local unit tests, but platform runtime behavior is not yet guaranteed. |
| Experimental | API exists to explore support; expect changes before it is recommended. |

## Current Validation Matrix

| Platform / adapter | Module | Status | Notes |
| --- | --- | --- | --- |
| Fabric client commands | `buildmycommand-adapters-minecraft-fabric` | Tested | Validated in a real Fabric client-side mod. Route dispatch, Brigadier registration, aliases, suggestions, invalid input behavior, and vanilla-serializable Brigadier argument trees have been exercised. |
| Brigadier | `buildmycommand-adapters-brigadier` | Tested | Validated through the Fabric adapter path, because Fabric client commands are Brigadier-backed. Adapter tests assert fallback tunnel nodes use vanilla `StringArgumentType`, not anonymous/custom argument types. |
| Minecraft common | `buildmycommand-adapters-minecraft-common` | Contract-tested | Shared Minecraft source, permissions, suggestion, and formatting helpers used by platform adapters. |
| Paper | `buildmycommand-adapters-minecraft-paper` | Compile-tested | Module exists and compiles; needs a real Paper plugin smoke test before being recommended as production-ready. |
| Spigot | `buildmycommand-adapters-minecraft-spigot` | Compile-tested | Module exists and compiles; needs a real Spigot plugin smoke test. |
| BungeeCord | `buildmycommand-adapters-minecraft-bungee` | Compile-tested | Module exists and compiles; needs proxy runtime validation. |
| Velocity | `buildmycommand-adapters-minecraft-velocity` | Compile-tested | Module exists and compiles; needs proxy runtime validation. |
| Minestom | `buildmycommand-adapters-minecraft-minestom` | Compile-tested | Module exists and compiles; needs a real Minestom server smoke test. |
| Sponge | `buildmycommand-adapters-minecraft-sponge` | Compile-tested | Module exists and compiles; needs Sponge runtime validation. |
| Forge | `buildmycommand-adapters-minecraft-forge` | Compile-tested | Module exists and compiles; needs a real Forge mod smoke test. |
| NeoForge | `buildmycommand-adapters-minecraft-neoforge` | Compile-tested | Module exists and compiles; needs a real NeoForge mod smoke test. |
| Terminal | `buildmycommand-adapters-terminal` | Contract-tested | Useful for examples, tests, and CLI-like environments. Not a Minecraft platform. |
| Discord | `buildmycommand-adapters-discord` | Compile-tested | Module exists and compiles; needs real Discord integration validation. |

## Recommended Platform Today

For Minecraft usage today, the recommended path is Fabric client commands or a direct Brigadier integration.

```kotlin
dependencies {
    implementation("io.github.zolkers:buildmycommand-adapters-minecraft-fabric:<version>")
}
```

If your platform is not listed as `Tested`, use it as an integration target only after running your own smoke test for:

| Behavior | Why it matters |
| --- | --- |
| Command registration | Confirms the native platform actually exposes the command. |
| Unknown command handling | Ensures BuildMyCommand does not swallow unrelated platform commands. |
| Incomplete input errors | Ensures missing arguments fail clearly. |
| Suggestions | Confirms literals, aliases, dynamic suggestions, and parser suggestions appear correctly. |
| Permissions / requirements | Confirms `@Permission` and `@Require` map to the platform source. |
| Case sensitivity | Confirms literal and option policies match the configured framework behavior. |
| Middleware / exceptions | Confirms platform replies and error mapping are visible to users. |

## Graduation Rule

A platform moves to `Tested` only when it has either:

1. A real example project checked manually against the platform runtime.
2. An automated end-to-end smoke test that launches enough of the platform to prove registration, dispatch, suggestions, and errors.

Until then, modules can still be useful, but they should not be documented as production-proven.
