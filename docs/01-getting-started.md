<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Getting Started

The shortest path is:

1. Create a `CommandFramework`.
2. Register an annotated command object.
3. Dispatch input from your platform adapter.

```java
CommandFramework framework = CommandFramework.create();
AnnotationCommandScanner.register(framework.registry(), new PingCommand());

CommandResult result = framework.dispatch(source, "wecc ping");
```

## Canonical Command Style

Use `@Command` for the root and `@SubRoute` for executable leaves.

```java
@Command("wecc")
@CaseInsensitive
public final class PingCommand {
    @SubRoute("ping")
    @Description("Client command smoke test")
    CommandResult ping(@RouteCtx CommandContext ctx) {
        return Results.success("pong from client");
    }

    @SubRoute("bang|b <target:String>")
    @SuggestAliases(false)
    CommandResult bang(@RouteCtx CommandContext ctx) {
        return Results.success("bang to " + ctx.arg("target", String.class));
    }
}
```

This style is preferred because the command shape stays in one route string. Deep nesting remains readable, aliases stay next to the literal they alias, and IntelliJ can inspect the DSL.

## Required Dependencies

```kotlin
repositories {
    mavenCentral()
}

val buildMyCommandVersion = "0.2.0"

dependencies {
    implementation("io.github.zolkers:buildmycommand-api:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-core:$buildMyCommandVersion")
    implementation("io.github.zolkers:buildmycommand-annotations:$buildMyCommandVersion")
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
</dependencies>
```

Add one adapter for your runtime:

```kotlin
implementation("io.github.zolkers:buildmycommand-adapters-minecraft-fabric:$buildMyCommandVersion")
implementation("io.github.zolkers:buildmycommand-adapters-brigadier:$buildMyCommandVersion")
implementation("io.github.zolkers:buildmycommand-adapters-terminal:$buildMyCommandVersion")
```


Maven adapter example:

```xml
<dependency>
    <groupId>io.github.zolkers</groupId>
    <artifactId>buildmycommand-adapters-minecraft-fabric</artifactId>
    <version>${buildmycommand.version}</version>
</dependency>
```

The complete adapter artifact table lives in [Adapters](05-adapters.md). Most applications need exactly one adapter artifact. Minecraft projects usually use one loader/platform artifact, while libraries that integrate their own command system can depend on `adapters-core` and implement their own adapter.

## Custom Argument Types

Register custom DSL types at framework creation time. This is the recommended path for platform objects such as `Material`, `ItemStack`, `World`, `PlayerProfile`, or your own domain objects.

```java
CommandFramework framework = CommandFramework.builder()
    .type("Material", Material.class, new MaterialParser())
    .build();

AnnotationCommandScanner.register(framework.registry(), new ShopCommands());
```

Then use the alias in routes:

```java
@Command("shop")
final class ShopCommands {
    @SubRoute("give <item:Material>")
    CommandResult give(@RouteCtx CommandContext ctx) {
        Material item = ctx.arg("item", Material.class);
        return Results.success("giving " + item);
    }
}
```

The parser owns validation and suggestions:

```java
final class MaterialParser implements ArgumentParser<Material> {
    @Override
    public ArgumentParseResult<Material> parse(String rawToken, ArgumentParseContext ctx) {
        return registry.find(rawToken)
            .map(ArgumentParseResult::success)
            .orElseGet(() -> ArgumentParseResult.failure("Unknown material: " + rawToken));
    }

    @Override
    public List<Suggestion> suggestions(ArgumentParseContext ctx) {
        return registry.idsStartingWith(ctx.rawToken()).stream()
            .map(id -> new Suggestion(id, Optional.of("material"), ctx.replacementStart(), ctx.replacementEnd(),
                SuggestionType.ARGUMENT, 0))
            .toList();
    }
}
```

Use `.types(types -> types.register(...))` when registering several aliases in one place.

The IntelliJ plugin understands the same Java setup. If a project contains `.type("Material", Material.class, parser)` or `.types(types -> types.register("Material", Material.class, parser))`, routes such as `<item:Material>` are accepted by the DSL annotator without extra IDE configuration. Keep type registrations in a small setup class when possible; it is easier for humans to find and for the IDE to index.

## CommandSource

`CommandSource` is your bridge from the native runtime to BuildMyCommand.

It answers:

| Method | Purpose |
| --- | --- |
| `name()` | Optional display name for help, logging, middleware, replies. |
| `hasPermission(String)` | Exact permission check used by `@Permission` and `@Require`. |
| `permissions()` | Optional permission set used by regex permissions. |
| `hasPermissionMatching(Pattern)` | Optional regex permission matcher. |
| `reply(CommandMessage)` | Platform-specific reply rendering. |
| `unwrap(Class<T>)` | Access to the native source for suggestions or platform logic. |

For Fabric client commands, wrap `FabricClientCommandSource` once and keep command classes clean. See [Minecraft/Fabric](06-minecraft.md).

Regex permissions are opt-in:

```java
@Permission(value = "admin\\.audit\\..*", regex = true)
@SubRoute("audit")
CommandResult audit(@RouteCtx CommandContext ctx) {
    return Results.success("audit");
}
```

They match against `CommandSource.permissions()` by default, or against your
custom `hasPermissionMatching(Pattern)` override.

## Dispatch Results

Use `Results.success(...)`, `Results.failure(...)`, or `Results.silent()`.

```java
CommandResult result = framework.dispatch(source, "wecc ping");

result.reply().ifPresent(System.out::println);
```

Most adapters should send replies through `CommandSource.reply(CommandMessage)` so commands do not know about Minecraft chat, terminals, or bot messages.
