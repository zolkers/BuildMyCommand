<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# BuildMyCommand

[![CI](https://github.com/zolkers/BuildMyCommand/actions/workflows/ci.yml/badge.svg)](https://github.com/zolkers/BuildMyCommand/actions/workflows/ci.yml)
[![Release](https://github.com/zolkers/BuildMyCommand/actions/workflows/release.yml/badge.svg)](https://github.com/zolkers/BuildMyCommand/actions/workflows/release.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.zolkers/buildmycommand-core.svg)](https://central.sonatype.com/artifact/io.github.zolkers/buildmycommand-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

BuildMyCommand is a modular Java command framework built around a readable route DSL, annotation-first command declarations, dynamic suggestions, middleware, permissions, custom argument types, and platform adapters.

The recommended style is intentionally compact: define a command root with `@Command`, define executable leaves with `@SubRoute`, receive a typed `CommandContext` through `@RouteCtx`, and keep the route string as the source of truth.

```java
@Command("wecc")
@CaseInsensitive
public final class PingCommand {
    @SubRoute("ping")
    CommandResult ping(@RouteCtx CommandContext ctx) {
        return Results.success("pong");
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
            .map(players -> SuggestionSet.of(players.stream().map(String::valueOf).toList())
                .filteringCurrentToken())
            .orElseGet(SuggestionSet::empty);
    }
}
```

## Why BuildMyCommand

| Capability | What it gives you |
| --- | --- |
| Route DSL | One readable string for literals, aliases, arguments, options, flags, optional segments, and greedy arguments. |
| Annotation-first API | Clean command classes with `@Command`, `@SubRoute`, `@RouteCtx`, `@Suggest`, `@Permission`, `@Require`, and middleware annotations. |
| Dynamic suggestions | Static or runtime completions through `SuggestionSet` and `SuggestionContext`. |
| Permission model | Exact permissions, regex permissions, boolean requirements, and source-aware filtering. |
| Custom types | Register aliases like `Material`, `ItemStack`, `Rank`, or any project type once in the framework builder. |
| Middleware | Add cross-cutting behavior such as permission gates, logging, formatting, cooldowns, and result decoration. |
| Help toolkit | Build your own help command with pagination, grouping, filtering, details, and custom formatting. |
| Adapters | Use the same command model on Fabric, Brigadier, terminal apps, and other supported platforms. |
| IntelliJ tooling | DSL highlighting and inspections for routes, requirements, permissions, and custom type aliases. |

## Installation

Use the latest released version on Maven Central.

### Gradle Kotlin

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.3.3"

dependencies {
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion")

    // Pick the adapter for your runtime.
    implementation("io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion")
}
```

### Gradle Groovy

```groovy
repositories {
    mavenCentral()
}

def buildMyCommandVersion = "0.3.3"

dependencies {
    implementation "io.github.zolkers:buildmycommand-api:$buildMyCommandVersion"
    implementation "io.github.zolkers:buildmycommand-core:$buildMyCommandVersion"
    implementation "io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion"

    // Pick the adapter for your runtime.
    implementation "io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion"
}
```

### Maven

```xml
<properties>
    <buildmycommand.version>0.3.3</buildmycommand.version>
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

For non-Fabric projects, replace the Fabric adapter with the adapter that matches your runtime. See [Adapters](docs/integrations/adapters.md).

## Quick Start

Create a framework, register annotated command classes, then connect the framework to your platform adapter.

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();

AnnotationCommandScanner.register(framework.registry(), new PingCommand());
```

For Fabric client commands, use the Fabric adapter and wrap the native source in your own `CommandSource` implementation when you need custom replies, permissions, or native access.

```java
FabricBrigadierRegistration<FabricClientCommandSource> registration =
    FabricMinecraftIntegration.registration(framework, ModCommandSource::client);

ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
    registration.registerInto(dispatcher);
});
```

`CommandSource` is the framework boundary between your platform and BuildMyCommand. It lets commands reply, check permissions, expose metadata, and unwrap the native platform source when a command or suggestion provider needs it.

## Route DSL

Routes describe command shape in one place:

```java
@Command("admin")
@CaseInsensitive(literals = true, options = true)
public final class AdminCommands {
    @SubRoute("moderation|mod punish temporary|temp add <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    @Permission("admin.moderation.punish.temp.add")
    CommandResult addTemporaryPunishment(@RouteCtx CommandContext route) {
        return Results.success(
            "temporary punishment for "
                + route.arg("target", String.class)
                + ": "
                + route.arg("reason", String.class)
        );
    }
}
```

| Syntax | Meaning |
| --- | --- |
| `moderation punish` | Required literals. |
| `moderation|mod` | Literal alias. |
| `<target:String>` | Required typed argument. |
| `<reason:String...>` | Greedy typed argument. |
| `[target:String]` | Optional positional argument. |
| `[--duration:Integer|-d]` | Optional typed option with short alias. |
| `[--silent|-s]` | Boolean flag with short alias. |

The DSL is the canonical command declaration model. Parameter annotations such as `@Arg` and `@Option` are still available for legacy or highly explicit code, but `@RouteCtx CommandContext` with `@Route`/`@SubRoute` is the recommended path.

## Custom Argument Types

Register project-specific types once in the framework builder, then use them directly in route strings.

```java
CommandFramework framework = CommandFramework.builder()
    .type("Material", Material.class, new MaterialParser())
    .type("ItemStack", ItemStack.class, new ItemStackParser())
    .build();

@Command("shop")
public final class ShopCommands {
    @SubRoute("give <item:Material>")
    CommandResult give(@RouteCtx CommandContext ctx) {
        Material item = ctx.arg("item", Material.class);
        return Results.success("giving " + item);
    }
}
```

Suggestion providers can be registered with the parser or supplied separately through `@Suggest`.

## Help Commands

BuildMyCommand provides a public help provider API, not a forced universal command. You choose the route, text style, platform messages, and formatting.

```java
AnnotationCommandScanner.register(framework.registry(), new HelpCommands(framework.helpProvider()));

@CommandGroup("System")
final class HelpCommands {
    private final HelpProviderAPI help;

    HelpCommands(HelpProviderAPI help) {
        this.help = help;
    }

    @Route(HelpProviderAPI.DEFAULT_ROUTE)
    CommandResult help(@RouteCtx CommandContext ctx) {
        return Results.success(help.render(
            ctx.source(),
            ctx.optionalArg("query", String.class).orElse(""),
            HelpOptions.from(ctx)
        ));
    }
}
```

`HelpProviderAPI` supports permission-aware listings, command details, pagination, alphabetical sorting, group filtering, command suggestions, group suggestions, and custom `HelpFormatter` implementations.

## Modules

| Artifact | Purpose |
| --- | --- |
| `buildmycommand-api` | Public contracts: context, source, result, messages, suggestions, middleware, help provider API. |
| `buildmycommand-core` | Registry, parser, dispatcher, help generation, schema export, permissions, middleware chain. |
| `buildmycommand-annotations` | Annotation model and scanner. |
| `buildmycommand-dsl` | Route parser model shared by runtime and tooling. |
| `buildmycommand-adapters-core` | `IAdapter` contract and shared adapter concepts. |
| `buildmycommand-adapters-brigadier` | Brigadier bridge used by Brigadier-based runtimes. |
| `buildmycommand-adapters-minecraft-common` | Shared Minecraft adapter utilities. |
| `buildmycommand-adapters-minecraft-fabric` | Fabric Brigadier registration helpers. |
| `buildmycommand-adapters-terminal` | Terminal integration. |
| `intellij-plugin` | JetBrains plugin module for route DSL highlighting, inspections, and IDE integration. |

Additional Minecraft adapters are documented in [Platform Adapters](docs/integrations/adapters.md). Current platform validation status is tracked in [Platform Info](PLATFORM-INFO.md).

## Documentation

| Goal | Documentation |
| --- | --- |
| Install the framework | [Installation](docs/getting-started/installation.md) |
| Learn the DSL | [Route DSL](docs/concepts/route-dsl.md) |
| Use annotations | [Annotations](docs/concepts/annotations.md) |
| Use the builder API | [Builder API](docs/concepts/builder-api.md) |
| Add middleware, permissions, errors | [Middleware, Permissions, Errors](docs/features/middleware-permissions-errors.md) |
| Build a help command | [Help Commands](docs/features/help-commands.md) |
| Integrate a platform | [Adapters](docs/integrations/adapters.md), [Minecraft](docs/integrations/minecraft.md) |
| Install IDE support | [IntelliJ Plugin](docs/tooling/intellij-plugin.md) |
| Test commands and adapters | [Testing](docs/tooling/testing.md) |
| Maintain releases | [Publishing](docs/maintainers/publishing.md) |

Start with [Documentation Home](docs/README.md) for the full guide index.

## Development

Run the full verification suite:

```powershell
.\gradlew.bat clean check
```

The repository enforces license headers, style checks, unit tests, adapter smoke tests, IntelliJ plugin tests, and coverage verification for publishable modules.

## Release Status

BuildMyCommand is pre-1.0. The canonical API is already centered on `@Command`, `@SubRoute`, `@RouteCtx`, `@Suggest`, `@Permission`, `@Require`, custom type registration, middleware, and adapter contracts, but breaking changes can still happen before `1.0.0`.

For production projects, pin an exact version and read the release notes before upgrading.

## Contributing

Contributions are welcome. Please read:

| File | Purpose |
| --- | --- |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Development workflow and contribution expectations. |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community standards. |
| [SECURITY.md](SECURITY.md) | Responsible vulnerability reporting. |
| [LICENSE](LICENSE) | MIT license. |
| [HEADER.txt](HEADER.txt) | Source header convention. |
