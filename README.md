# BuildMyCommand

BuildMyCommand is a modular Java command framework for applications, terminals, bots, and Minecraft-like platforms. It gives you one command model, one parsing/runtime layer, one adapter contract, and multiple ways to declare commands.

The recommended style is **annotation routes**:

```java
@Command("moderation")
static final class ModerationCommands {
    @SubRoute("punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    @Description("Punish a player")
    @Permission("mod.punish")
    CommandResult punish(@RouteCtx CommandContext ctx) {
        String target = ctx.arg("target", String.class);
        String reason = ctx.arg("reason", String.class);
        int duration = ctx.option("duration", Integer.class).orElse(60);
        boolean silent = ctx.flag("silent");
        return Results.success("Punished " + target + " for " + duration + "m silent=" + silent + ": " + reason);
    }
}
```

`@Route` and `@SubRoute` are preferred because the command shape is visible in one place, deeply nested commands stay readable, aliases and options stay close to the path, and the IntelliJ plugin can highlight/inspect the DSL.

## Modules

Artifacts are prepared for Maven Central with group `io.github.zolkers` and version `0.0.1`.

| Artifact | Gradle project | Purpose |
| --- | --- | --- |
| `api` | `:api` | Public contracts: command graph, context, results, metadata, middleware, exceptions. |
| `core` | `:core` | Runtime registry, parser, dispatcher, help, suggestions, permissions, lifecycle. |
| `annotations` | `:annotations` | `@Route`, `@SubRoute`, `@Command`, `@Permission`, `@Require`, scanners and binders. |
| `dsl` | `:dsl` | Route DSL parser model used by core and tooling. |
| `schema` | `:schema` | Schema/export helpers for command metadata. |
| `testkit` | `:testkit` | Test utilities for framework users. |
| `adapters-core` | `:adapters:core` | `IAdapter`, source/input/rendering contracts, generic adapter runtime. |
| `adapters-terminal` | `:adapters:terminal` | Terminal adapter. |
| `adapters-discord` | `:adapters:discord` | Discord-style adapter foundation. |
| `adapters-brigadier` | `:adapters:brigadier` | Generic Mojang Brigadier bridge. |
| `adapters-minecraft-common` | `:adapters:minecraft:common` | Shared Minecraft adapter contracts and capability model. |
| `adapters-minecraft-spigot` | `:adapters:minecraft:spigot` | Spigot integration layer. |
| `adapters-minecraft-paper` | `:adapters:minecraft:paper` | Paper integration layer. |
| `adapters-minecraft-bungee` | `:adapters:minecraft:bungee` | BungeeCord integration layer. |
| `adapters-minecraft-velocity` | `:adapters:minecraft:velocity` | Velocity integration layer. |
| `adapters-minecraft-minestom` | `:adapters:minecraft:minestom` | Minestom integration layer. |
| `adapters-minecraft-sponge` | `:adapters:minecraft:sponge` | Sponge integration layer. |

## Install

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.zolkers:api:0.0.1")
    implementation("io.github.zolkers:core:0.0.1")
    implementation("io.github.zolkers:annotations:0.0.1")
}
```

Add only the adapter modules you need:

```kotlin
implementation("io.github.zolkers:adapters-terminal:0.0.1")
implementation("io.github.zolkers:adapters-brigadier:0.0.1")
implementation("io.github.zolkers:adapters-minecraft-paper:0.0.1")
```

## Documentation

| File | Read when |
| --- | --- |
| [01 Getting Started](docs/01-getting-started.md) | You want the shortest path to a working command. |
| [02 Route And SubRoute](docs/02-route-and-subroute.md) | You want the recommended command declaration style. |
| [03 Annotations](docs/03-annotations.md) | You need every annotation, target, and interaction explained. |
| [04 Builder API](docs/04-builder-api.md) | You need dynamic/programmatic command trees. |
| [05 Adapters](docs/05-adapters.md) | You want to plug the framework into another runtime. |
| [06 Minecraft](docs/06-minecraft.md) | You target Brigadier or Minecraft server/proxy platforms. |
| [07 IntelliJ Plugin](docs/07-intellij-plugin.md) | You want DSL highlighting, inspections, and setup scripts. |
| [08 Errors, Middleware, Permissions](docs/08-errors-middleware-permissions.md) | You need execution policies, guards, and failure handling. |
| [09 Testing](docs/09-testing.md) | You want to test commands/adapters safely. |
| [10 Publishing](docs/10-publishing.md) | You want Maven Central or IntelliJ Marketplace release notes. |

## Build

```powershell
.\gradlew.bat check
```

The repository enforces 100% JaCoCo coverage per publishable module, plus lightweight style/static checks.
