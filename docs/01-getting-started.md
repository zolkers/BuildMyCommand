# 01 - Getting Started

This page gets a command running quickly while explaining the minimum concepts you need to avoid fighting the framework.

## Recommended Dependency Set

| Need | Modules |
| --- | --- |
| Basic command framework | `api`, `core` |
| Annotation commands | `annotations` |
| IntelliJ DSL support | local IntelliJ plugin from `:intellij-plugin` |
| Terminal runtime | `adapters-terminal` |
| Minecraft/Paper runtime | `adapters-minecraft-paper` plus required platform API |
| Custom runtime | `adapters-core` |

```kotlin
dependencies {
    implementation("dev.riege.buildmycommand:api:0.0.1")
    implementation("dev.riege.buildmycommand:core:0.0.1")
    implementation("dev.riege.buildmycommand:annotations:0.0.1")
}
```

## Minimal Route Command

Use `@Route` for standalone commands:

```java
final class KitCommands {
    @Route("kit <target:String> <name:String> [--amount:Integer|-a] [--silent|-s]")
    @Description("Give a kit")
    @Permission("kit.give")
    CommandResult kit(@RouteCtx CommandContext ctx) {
        String target = ctx.arg("target", String.class);
        String name = ctx.arg("name", String.class);
        int amount = ctx.option("amount", Integer.class).orElse(1);
        boolean silent = ctx.flag("silent");
        return Results.success("Giving " + amount + "x " + name + " to " + target + " silent=" + silent);
    }
}
```

Register and dispatch:

```java
CommandFramework framework = CommandFramework.create();
AnnotationCommandScanner.register(framework.registry(), new KitCommands());

CommandResult result = framework.dispatch(source, "kit Ada starter -a 3 -s");
```

## Minimal Group With SubRoute

Use `@Command` on the class and `@SubRoute` on methods when several commands share a root:

```java
@Command("user")
@Alias("u")
static final class UserCommands {
    @SubRoute("rank set <target:String> <rank:String> [--temporary|-t]")
    @Permission("user.rank.set")
    CommandResult setRank(@RouteCtx CommandContext ctx) {
        return Results.success(ctx.arg("target", String.class) + " -> " + ctx.arg("rank", String.class));
    }
}
```

This registers both `user rank set ...` and `u rank set ...`.

## Core Runtime Objects

| Type | Role |
| --- | --- |
| `CommandFramework` | Main facade. Owns registry, dispatcher, suggestions, help, lifecycle. |
| `CommandRegistry` | Registration surface used by annotations, builders, routes, adapters. |
| `CommandSource` | Caller abstraction. Holds permissions, name/id, locale, metadata, reply hooks. |
| `CommandInput` | Raw/normalized input, cursor, prefix, platform metadata. |
| `CommandContext` | Runtime values for args/options/flags plus source/input. |
| `CommandResult` | `SUCCESS`, `FAILURE`, or `SILENT`, optionally with a `CommandMessage`. |

## Result Helpers

| Helper | Result |
| --- | --- |
| `Results.success("ok")` | Successful reply. |
| `Results.failure("no")` | Failed reply. |
| `Results.silent()` | No reply, no visible failure. |

## CommandSource Permissions

`CommandSource.hasPermission(String)` defaults to `true`. Production adapters should override it:

```java
CommandSource source = new CommandSource() {
    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
};
```

## First Checks When Something Does Not Work

| Symptom | Check |
| --- | --- |
| Annotation command is not found | Did you call `AnnotationCommandScanner.register(...)`? |
| Parameters are not bound | Prefer `@RouteCtx CommandContext` for route methods, or compile with `-parameters` for name inference. |
| Permission denied unexpectedly | Check source `hasPermission`, then `@Permission` and `@Require`. |
| DSL not colored in IntelliJ | Install/reinstall the local plugin and restart IntelliJ. |
| Optional argument parse issue | In route DSL, required args must not follow optional args. |

## Recommended Learning Order

| Step | Doc |
| --- | --- |
| 1 | [Route and SubRoute](02-route-and-subroute.md) |
| 2 | [Annotations](03-annotations.md) |
| 3 | [Errors, Middleware, Permissions](08-errors-middleware-permissions.md) |
| 4 | [Adapters](05-adapters.md) |
