# BuildMyCommand

BuildMyCommand is a universal, platform-neutral Java command framework.

The project follows a simple rule: every public declaration style eventually compiles to the same internal command model. The first implementation slice focuses on the core runtime:

- `api` for public contracts
- `core` for registration, routing, dispatch, parsing, and suggestions
- `minecraft-adapter` for version-neutral Minecraft integration primitives
- `intellij-plugin` for IDE support around annotations and route DSL strings
- `testkit` for platform-free command tests

The long-form architecture plan is tracked in `java_command_framework_master_plan (1).md`.

## Build

```powershell
.\gradlew.bat test
```

## Current Scope

The repository is being built incrementally from a minimal command dispatcher toward the full roadmap:

1. literal command dispatch: implemented
2. typed arguments: implemented
3. subcommands and aliases: implemented
4. flags and options: in progress
5. suggestions and help: suggestions in progress, help later
6. DSL, annotations, adapters, and studio modules: in progress

## Architecture Notes

- Core runtime internals are grouped by responsibility under packages such as `registry`, `parse`, `dispatch`, `route`, and `help`.
- Minecraft support is intentionally loader-neutral. Paper, Fabric, NeoForge, and legacy Bukkit-style integrations should bind their version-specific registration APIs to `MinecraftCommandBridge` instead of leaking those APIs into `core`.
- IntelliJ support starts as an IntelliLang-based plugin scaffold. Rich validation and completion should be added inside `modules/intellij-plugin`, not in runtime modules.
