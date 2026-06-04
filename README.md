<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# BuildMyCommand

BuildMyCommand is a modular Java command framework. It gives you one command runtime, a readable route DSL, annotation scanning, dynamic suggestions, middleware, permissions, and adapters for runtimes such as Fabric/Brigadier.

## Getting Started

Add the dependencies first. Use the latest released version available on Maven Central.

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.2.0"

dependencies {
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion")
}
```


Maven:

```xml
<properties>
    <buildmycommand.version>0.2.0</buildmycommand.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-api</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-core</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-annotations</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.zolkers</groupId>
        <artifactId>buildmycommand-adapters-minecraft-fabric</artifactId>
        <version>${buildmycommand.version}</version>
    </dependency>
</dependencies>
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

Custom argument types are registered once when the framework is created. After that, routes can use friendly names such as `<item:Material>`, and IntelliJ recognizes them from the builder setup.

```java
CommandFramework framework = CommandFramework.builder()
    .type("Material", Material.class, new MaterialParser())
    .build();

@Command("shop")
final class ShopCommands {
    @SubRoute("give <item:Material>")
    CommandResult give(@RouteCtx CommandContext ctx) {
        Material item = ctx.arg("item", Material.class);
        return Results.success("giving " + item);
    }
}
```

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
| [Adapters](docs/05-adapters.md) | Adapter artifacts, `IAdapter`, and custom adapter examples. |
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

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), [SECURITY.md](SECURITY.md), and [LICENSE](LICENSE). Source, docs, scripts, and configuration files that support comments carry the MIT SPDX header from [HEADER.txt](HEADER.txt).
