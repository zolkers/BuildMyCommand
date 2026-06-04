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

val buildMyCommandVersion = "0.1.0"

dependencies {
    implementation("io.github.zolkers:api:$buildMyCommandVersion")
    implementation("io.github.zolkers:core:$buildMyCommandVersion")
    implementation("io.github.zolkers:annotations:$buildMyCommandVersion")
}
```

Add one adapter for your runtime:

```kotlin
implementation("io.github.zolkers:adapters-minecraft-fabric:$buildMyCommandVersion")
implementation("io.github.zolkers:adapters-brigadier:$buildMyCommandVersion")
implementation("io.github.zolkers:adapters-terminal:$buildMyCommandVersion")
```

The complete adapter artifact table lives in [Adapters](05-adapters.md). Most applications need exactly one adapter artifact. Minecraft projects usually use one loader/platform artifact, while libraries that integrate their own command system can depend on `adapters-core` and implement their own adapter.

## CommandSource

`CommandSource` is your bridge from the native runtime to BuildMyCommand.

It answers:

| Method | Purpose |
| --- | --- |
| `name()` | Optional display name for help, logging, middleware, replies. |
| `hasPermission(String)` | Permission check used by `@Permission` and `@Require`. |
| `reply(CommandMessage)` | Platform-specific reply rendering. |
| `unwrap(Class<T>)` | Access to the native source for suggestions or platform logic. |

For Fabric client commands, wrap `FabricClientCommandSource` once and keep command classes clean. See [Minecraft/Fabric](06-minecraft.md).

## Dispatch Results

Use `Results.success(...)`, `Results.failure(...)`, or `Results.silent()`.

```java
CommandResult result = framework.dispatch(source, "wecc ping");

result.reply().ifPresent(System.out::println);
```

Most adapters should send replies through `CommandSource.reply(CommandMessage)` so commands do not know about Minecraft chat, terminals, or bot messages.
