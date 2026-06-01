# BuildMyCommand

BuildMyCommand is a universal, platform-neutral Java command framework.

The project follows a simple rule: every public declaration style eventually compiles to the same internal command model. The first implementation slice focuses on the core runtime:

- `api` for public contracts
- `core` for registration, routing, dispatch, parsing, and suggestions
- `testkit` for platform-free command tests

The long-form architecture plan is tracked in `java_command_framework_master_plan (1).md`.

## Build

```powershell
.\gradlew.bat test
```

## Current Scope

The repository is being built incrementally from a minimal command dispatcher toward the full roadmap:

1. literal command dispatch
2. typed arguments
3. subcommands and aliases
4. flags and options
5. suggestions and help
6. DSL, annotations, adapters, and studio modules
