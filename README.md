# BuildMyCommand

BuildMyCommand is a modular Java command framework. It gives you one command runtime, a readable route DSL, annotation scanning, dynamic suggestions, middleware, permissions, and adapters for runtimes such as Fabric/Brigadier.

## Getting Started

Add the dependencies first. Use the latest released version available on Maven Central.

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.1.0"

dependencies {
    implementation("io.github.zolkers:api:$buildMyCommandVersion")
    implementation("io.github.zolkers:core:$buildMyCommandVersion")
    implementation("io.github.zolkers:annotations:$buildMyCommandVersion")
    implementation("io.github.zolkers:adapters-minecraft-fabric:$buildMyCommandVersion")
}
```

For a plain Java app, replace the Fabric adapter with `adapters-terminal` or `adapters-brigadier`.

Then declare commands with the canonical annotation style:

```java
@Command("wecc")
@CaseInsensitive
public final class PingCommand {
    @SubRoute("ping")
    CommandResult ping(@RouteCtx CommandContext ctx) {
        return Results.success("pong from client");
    }

    @SuggestAliases(false)
    @SubRoute("bang|b <target:String>")
    CommandResult bang(@RouteCtx CommandContext ctx) {
        return Results.success("bang to " + ctx.arg("target", String.class));
    }

    @Suggest("target")
    SuggestionSet onlinePlayers(SuggestionContext ctx) {
        return ctx.sourceMetadata("players")
            .filter(List.class::isInstance)
            .map(List.class::cast)
            .map(players -> SuggestionSet.of(players.stream().map(String::valueOf).toList()).filteringCurrentToken())
            .orElseGet(SuggestionSet::empty);
    }
}
```

`@Command` declares the root. `@SubRoute` declares executable leaves. The route string is the source of truth for arguments, options, aliases, and optional segments. Parameter annotations such as `@Arg`/`@Option` are intentionally not the canonical style anymore.

## Modules

| Artifact | Purpose |
| --- | --- |
| `api` | Public contracts: context, source, result, messages, suggestions, middleware. |
| `core` | Registry, parser, dispatcher, help, schema, permissions, middleware chain. |
| `annotations` | `@Command`, `@Route`, `@SubRoute`, `@Suggest`, scanner/compiler. |
| `dsl` | Route parser model shared by runtime and tooling. |
| `adapters-core` | Generic `IAdapter` contract and shared adapter concepts. |
| `adapters-brigadier` | Brigadier bridge used by Minecraft-style platforms. |
| `adapters-minecraft-fabric` | Fabric registration helpers. |
| `adapters-terminal` | Terminal integration. |
| `intellij-plugin` | Route/requirement highlighting, inspections, and local IDE setup. |

## Documentation

| File | Content |
| --- | --- |
| [Getting Started](docs/01-getting-started.md) | Minimal framework setup and command registration. |
| [Route And SubRoute](docs/02-route-and-subroute.md) | The recommended DSL style. |
| [Annotations](docs/03-annotations.md) | Annotation contracts and IntelliJ rules. |
| [Builder API](docs/04-builder-api.md) | Programmatic command trees when annotations are not enough. |
| [Adapters](docs/05-adapters.md) | `IAdapter` concepts and custom adapter shape. |
| [Minecraft/Fabric](docs/06-minecraft.md) | Fabric client setup, `ModCommandSource`, and suggestions. |
| [IntelliJ Plugin](docs/07-intellij-plugin.md) | Local plugin install, highlighting, inspections. |
| [Middleware And Permissions](docs/08-errors-middleware-permissions.md) | `@Middleware`, `@Require`, `@Permission`, errors. |
| [Testing](docs/09-testing.md) | Command tests and adapter smoke tests. |
| [Publishing](docs/10-publishing.md) | Maven Central and local publishing notes. |

## Build

```powershell
.\gradlew.bat clean check
```

The repo currently enforces full test coverage for publishable modules.
