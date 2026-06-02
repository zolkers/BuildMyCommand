# BuildMyCommand

BuildMyCommand is a modular Java command framework for applications, CLIs, Minecraft servers, Discord bots, and IDE-aware command DSLs.

Every declaration style compiles to the same public command model:

- `modules/api`: stable command contracts, sources, results, metadata, suggestions, middleware, and manual command nodes.
- `modules/core`: registration, dispatch, parsing, matching policy, help, schema, lifecycle, and middleware execution.
- `modules/dsl`: route DSL parsing, canonicalization, validation, aliases, options, and conflict analysis.
- `modules/annotations`: annotation scanner/compiler for route and method-based command declarations.
- `modules/adapters/core`: generic adapter SDK for first-party and custom adapters.
- `modules/adapters/brigadier`: Mojang Brigadier tree projection, suggestions, aliases, permissions, and dispatch bridge.
- `modules/adapters/terminal`: terminal adapter and legacy terminal package shim.
- `modules/adapters/discord`: non-Minecraft adapter proving platform independence.
- `modules/adapters/minecraft`: Minecraft adapter family and platform modules.
- `modules/intellij-plugin`: route DSL injection, highlighting, completion, inspections, theme resources, and install scripts.
- `modules/testkit`: fluent command testing helpers.

Start with [Getting Started](docs/getting-started.md), then use the focused guides under `docs/`.

## Quick Example

```java
@Route("moderation punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
@Description("Punish a user")
@Permission("mod.punish")
CommandResult punish(
    @RouteCtx CommandContext route
) {
    String target = route.arg("target", String.class);
    String reason = route.arg("reason", String.class);
    int minutes = route.option("duration", Integer.class).orElse(60);
    return Results.success(target + " punished for " + minutes + "m: " + reason);
}
```

## Matching Policy

Matching is strict by default. Platforms that need friendlier casing can opt in explicitly:

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();
```

Literal matching covers command names, subcommands, and aliases. Option matching covers long and short options. Argument values are never case-normalized.

Annotation-first command sets can opt in from the declaration side:

```java
@CaseInsensitive
final class ModerationCommands {
    @Route("ban <target:String> [--silent|-s]")
    CommandResult ban(@RouteCtx CommandContext route) {
        String target = route.arg("target", String.class);
        boolean silent = route.flag("silent");
        return Results.success(target + ":" + silent);
    }
}
```

## Build

```powershell
.\gradlew.bat test
```

Useful verification commands:

```powershell
.\gradlew.bat clean check
.\gradlew.bat :intellij-plugin:buildPlugin
```

## Documentation

- [Core Concepts](docs/core-concepts.md)
- [Route DSL](docs/route-dsl.md)
- [Annotations](docs/annotations.md)
- [Builder API](docs/builder-api.md)
- [Manual API](docs/manual-api.md)
- [Suggestions](docs/suggestions.md)
- [Errors And Middleware](docs/errors-and-middleware.md)
- [Adapter SDK](docs/adapter-sdk.md)
- [Testing](docs/testing.md)
- [IntelliJ Plugin](docs/intellij-plugin.md)
- [Minecraft Adapter](docs/minecraft-adapter.md)
